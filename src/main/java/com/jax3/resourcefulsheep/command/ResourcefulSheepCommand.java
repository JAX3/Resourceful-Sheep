package com.jax3.resourcefulsheep.command;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import com.mojang.brigadier.context.CommandContext;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /resourcefulsheep} admin commands (operator level 2).
 * <ul>
 *     <li>{@code generate} - creates a sheep resource for every material found in {@code c:storage_blocks/*} tags
 *     that is not already defined, writes them to the world data pack {@code resourcefulsheep_generated} and reloads.</li>
 *     <li>{@code generate all} - same, but also regenerates materials that already have a definition.</li>
 *     <li>{@code generate dedupe} - deletes generated resources that duplicate another wool type (same feeding
 *     item or same name, e.g. a generated Iron next to the built-in Iron) and reloads. Built-in and hand-written
 *     definitions are never removed.</li>
 *     <li>{@code generate material <material>} - adds one material from the storage-block tags (tab-completes).</li>
 *     <li>{@code generate item <item>} - adds a wool type fed with any item; {@code generate hand} uses the held item.</li>
 *     <li>{@code generate remove <name>} - deletes one generated wool type (tab-completes).</li>
 *     <li>{@code preview} - lists what {@code generate} would create, without writing anything.</li>
 *     <li>{@code list} - lists every loaded resource.</li>
 * </ul>
 */
public final class ResourcefulSheepCommand {
    private ResourcefulSheepCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static final SuggestionProvider<CommandSourceStack> MATERIALS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ResourceGenerator.storageMaterials(ctx.getSource().getServer().registryAccess()), builder);
    private static final SuggestionProvider<CommandSourceStack> GENERATED = (ctx, builder) ->
            SharedSuggestionProvider.suggest(SheepResourceRegistry.server().all().stream()
                    .filter(ResourceGenerator::isGenerated).map(r -> r.id().getPath()), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal(ResourcefulSheep.MOD_ID)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("generate")
                        .executes(ctx -> generate(ctx, false))
                        .then(Commands.literal("all").executes(ctx -> generate(ctx, true)))
                        .then(Commands.literal("dedupe").executes(ResourcefulSheepCommand::dedupe))
                        .then(Commands.literal("material")
                                .then(Commands.argument("material", StringArgumentType.word()).suggests(MATERIALS)
                                        .executes(ctx -> generateMaterial(ctx, StringArgumentType.getString(ctx, "material")))))
                        .then(Commands.literal("item")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> generateItem(ctx, ItemArgument.getItem(ctx, "item").item().value()))))
                        .then(Commands.literal("hand").executes(ctx -> generateItem(ctx,
                                ctx.getSource().getPlayerOrException().getMainHandItem().getItem())))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.word()).suggests(GENERATED)
                                        .executes(ctx -> remove(ctx, StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("preview").executes(ResourcefulSheepCommand::preview))
                .then(Commands.literal("list").executes(ResourcefulSheepCommand::list)));
    }

    private static int preview(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        List<ResourceGenerator.Generated> plan = ResourceGenerator.plan(server.registryAccess(), SheepResourceRegistry.server(), false);
        if (plan.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.resourcefulsheep.preview.none"), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.resourcefulsheep.preview.header", plan.size()).withStyle(ChatFormatting.GOLD), false);
        for (ResourceGenerator.Generated g : plan) {
            ctx.getSource().sendSuccess(() -> describe(g), false);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.resourcefulsheep.preview.footer").withStyle(ChatFormatting.GRAY), false);
        return plan.size();
    }

    private static int generate(CommandContext<CommandSourceStack> ctx, boolean all) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        List<ResourceGenerator.Generated> plan = ResourceGenerator.plan(server.registryAccess(), SheepResourceRegistry.server(), all);
        Path pack;
        try {
            pack = ResourceGenerator.write(server.getWorldPath(LevelResource.DATAPACK_DIR), plan);
        } catch (Exception e) {
            ResourcefulSheep.LOGGER.error("Failed to write generated sheep resources", e);
            source.sendFailure(Component.translatable("command.resourcefulsheep.generate.failed", e.getMessage()));
            return 0;
        }
        for (ResourceGenerator.Generated g : plan) {
            source.sendSuccess(() -> describe(g), false);
        }
        source.sendSuccess(() -> Component.translatable("command.resourcefulsheep.generate.done", plan.size(), pack.getFileName().toString())
                .withStyle(ChatFormatting.GREEN), true);
        reloadWithNewPacks(source);
        return plan.size();
    }

    private static int generateMaterial(CommandContext<CommandSourceStack> ctx, String material) {
        var registries = ctx.getSource().getServer().registryAccess();
        var generated = ResourceGenerator.forMaterial(registries, material);
        if (generated.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("command.resourcefulsheep.material.unknown", material));
            return 0;
        }
        return writeSingle(ctx.getSource(), generated.get());
    }

    private static int generateItem(CommandContext<CommandSourceStack> ctx, net.minecraft.world.item.Item item) {
        if (item == net.minecraft.world.item.Items.AIR) {
            ctx.getSource().sendFailure(Component.translatable("command.resourcefulsheep.item.empty"));
            return 0;
        }
        if (item instanceof com.jax3.resourcefulsheep.items.ResourceWoolItem) {
            ctx.getSource().sendFailure(Component.translatable("command.resourcefulsheep.item.wool"));
            return 0;
        }
        return writeSingle(ctx.getSource(), ResourceGenerator.forItem(ctx.getSource().getServer().registryAccess(), item));
    }

    /** Adds one generated definition (replacing a generated one of the same name) unless a real definition already covers it. */
    private static int writeSingle(CommandSourceStack source, ResourceGenerator.Generated generated) {
        MinecraftServer server = source.getServer();
        var existing = ResourceGenerator.coveredBy(server.registryAccess(), SheepResourceRegistry.server(), generated);
        if (existing.isPresent()) {
            source.sendFailure(Component.translatable("command.resourcefulsheep.item.covered",
                    existing.get().name(), existing.get().id().toString()));
            return 0;
        }
        try {
            ResourceGenerator.writeOne(server.getWorldPath(LevelResource.DATAPACK_DIR), generated);
        } catch (Exception e) {
            ResourcefulSheep.LOGGER.error("Failed to write generated sheep resource", e);
            source.sendFailure(Component.translatable("command.resourcefulsheep.generate.failed", e.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> describe(generated), false);
        source.sendSuccess(() -> Component.translatable("command.resourcefulsheep.single.done",
                ResourceGenerator.title(generated.name()), generated.name()).withStyle(ChatFormatting.GREEN), true);
        reloadWithNewPacks(source);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        var resource = SheepResourceRegistry.server().byId(net.minecraft.resources.Identifier.tryBuild(ResourceGenerator.NAMESPACE, name));
        if (resource.isEmpty()) {
            source.sendFailure(Component.translatable("command.resourcefulsheep.remove.unknown", name));
            return 0;
        }
        try {
            ResourceGenerator.delete(source.getServer().getWorldPath(LevelResource.DATAPACK_DIR), List.of(resource.get()));
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.resourcefulsheep.generate.failed", e.getMessage()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.resourcefulsheep.remove.done", resource.get().name()).withStyle(ChatFormatting.GREEN), true);
        reloadWithNewPacks(source);
        return 1;
    }

    private static int dedupe(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        List<ResourceGenerator.Duplicate> duplicates = ResourceGenerator.findDuplicates(SheepResourceRegistry.server());
        if (duplicates.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.resourcefulsheep.dedupe.none").withStyle(ChatFormatting.GREEN), false);
            return 0;
        }
        int removed;
        try {
            removed = ResourceGenerator.delete(server.getWorldPath(LevelResource.DATAPACK_DIR),
                    duplicates.stream().map(ResourceGenerator.Duplicate::resource).toList());
        } catch (Exception e) {
            ResourcefulSheep.LOGGER.error("Failed to delete duplicate sheep resources", e);
            source.sendFailure(Component.translatable("command.resourcefulsheep.generate.failed", e.getMessage()));
            return 0;
        }
        for (ResourceGenerator.Duplicate d : duplicates) {
            source.sendSuccess(() -> Component.literal(" - ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(d.resource().name().getString() + " (" + d.resource().id() + ")").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  duplicates " + d.keptInstead().id() + ": " + d.reason()).withStyle(ChatFormatting.GRAY)), false);
        }
        int count = removed;
        source.sendSuccess(() -> Component.translatable("command.resourcefulsheep.dedupe.done", count).withStyle(ChatFormatting.GREEN), true);
        reloadWithNewPacks(source);
        return removed;
    }

    /**
     * Same as vanilla {@code /reload}: re-scan the data pack folder, enable newly found packs (unless the player
     * disabled them) and reload. NeoForge then re-syncs the definitions, recipes and JEI to every player.
     */
    private static void reloadWithNewPacks(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        var repository = server.getPackRepository();
        java.util.Collection<String> current = repository.getSelectedIds();
        repository.reload();
        java.util.List<String> selected = new java.util.ArrayList<>(current);
        java.util.Collection<String> disabled = server.getWorldData().getDataConfiguration().dataPacks().getDisabled();
        for (String id : repository.getAvailableIds()) {
            if (!disabled.contains(id) && !selected.contains(id)) selected.add(id);
        }
        net.neoforged.neoforge.resource.ResourcePackLoader.reorderNewlyDiscoveredPacks(selected, current, repository);
        server.reloadResources(selected).exceptionally(t -> {
            ResourcefulSheep.LOGGER.warn("Reload after generating sheep resources failed", t);
            source.sendFailure(Component.translatable("commands.reload.failure"));
            return null;
        });
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var all = SheepResourceRegistry.server().all();
        ctx.getSource().sendSuccess(() -> Component.translatable("command.resourcefulsheep.list.header", all.size()).withStyle(ChatFormatting.GOLD), false);
        for (SheepResource r : all) {
            ctx.getSource().sendSuccess(() -> Component.literal(" - ").append(r.name().copy().withColor(r.woolColor()))
                    .append(Component.literal("  " + r.id()).withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return all.size();
    }

    private static Component describe(ResourceGenerator.Generated g) {
        return Component.literal(" + ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(ResourceGenerator.title(g.name())).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("  " + g.feeding() + " -> " + BuiltInRegistries.ITEM.getKey(g.output())
                        + " x" + g.uses() + " (" + g.category().getSerializedName() + ")").withStyle(ChatFormatting.GRAY));
    }
}
