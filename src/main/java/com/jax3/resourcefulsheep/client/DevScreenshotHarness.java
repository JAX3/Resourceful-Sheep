package com.jax3.resourcefulsheep.client;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import com.jax3.resourcefulsheep.blockentity.SeparatorInventory;
import com.jax3.resourcefulsheep.blocks.MechanicalSeparatorBlock;
import com.jax3.resourcefulsheep.entity.SheepInfusion;
import com.jax3.resourcefulsheep.entity.SheepInfusionEvents;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.registry.ModBlocks;
import com.jax3.resourcefulsheep.registry.ModItems;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.FileUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Developer tool: with {@code -Dresourcefulsheep.screenshotHarness=true} the client creates a flat test world,
 * stages sheep/machines, captures screenshots into {@code run/screenshots/} and quits. Used to visually verify the
 * mod's rendering in CI-like environments. It does nothing unless the system property is set.
 */
public final class DevScreenshotHarness {
    private static int tick = -1;
    private static boolean worldRequested;
    private static BlockPos origin = BlockPos.ZERO;

    private DevScreenshotHarness() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("resourcefulsheep.screenshotHarness")) return;
        ResourcefulSheep.LOGGER.warn("Resourceful Sheep screenshot harness enabled");
        NeoForge.EVENT_BUS.addListener(DevScreenshotHarness::onScreenOpened);
        NeoForge.EVENT_BUS.addListener(DevScreenshotHarness::onTick);
    }

    private static void onScreenOpened(ScreenEvent.Opening event) {
        if (worldRequested || !(event.getNewScreen() instanceof net.minecraft.client.gui.screens.TitleScreen
                || event.getNewScreen() instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen)) return;
        worldRequested = true;
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            try {
                LevelSettings settings = new LevelSettings("RS Harness", GameType.CREATIVE, LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
                String folder = FileUtil.findAvailableName(mc.getLevelSource().getBaseDir(), "RS Harness", "");
                mc.createWorldOpenFlows().createFreshLevel(folder, settings, new WorldOptions(42L, false, false),
                        WorldPresets::createTestWorldDimensions, new net.minecraft.client.gui.screens.TitleScreen());
            } catch (IOException e) {
                ResourcefulSheep.LOGGER.error("Harness could not create world", e);
            }
        });
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) return;
        tick++;
        mc.gui.toastManager().clear();
        switch (tick) {
            case 60 -> server.execute(() -> setupScene(server));
            case 90 -> command(server, "resourcefulsheep generate material slime");
            case 110 -> command(server, "resourcefulsheep generate item minecraft:amethyst_block");
            case 130 -> command(server, "resourcefulsheep generate item minecraft:iron_block");
            case 140 -> command(server, "resourcefulsheep generate item minecraft:emerald_block");
            case 150 -> server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BLOCK));
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack(), "resourcefulsheep generate hand");
            });
            case 175 -> command(server, "resourcefulsheep generate remove slime");
            case 190 -> setHudHidden(mc, true);
            case 200 -> shot(mc, "01_world_sheep");
            case 210 -> server.execute(() -> look(server, 180, 28));
            case 240 -> shot(mc, "02_separators");
            case 250 -> {
                setHudHidden(mc, false);
                command(server, "gamemode survival @a");
                mc.execute(() -> mc.gui.setScreen(new InventoryScreen(mc.player)));
            }
            case 290 -> shot(mc, "03_inventory_jei");
            case 300 -> {
                mc.gui.setScreen(null);
                server.execute(() -> openSeparator(server));
            }
            case 360 -> shot(mc, "04_separator_gui");
            case 370 -> {
                mc.player.closeContainer();
                if (ModList.get().isLoaded("jei")) {
                    com.jax3.resourcefulsheep.integration.jei.JeiDevHook.showCategory(true);
                }
            }
            case 410 -> shot(mc, "05_jei_infusion");
            case 420 -> {
                mc.gui.setScreen(null);
                if (ModList.get().isLoaded("jei")) {
                    com.jax3.resourcefulsheep.integration.jei.JeiDevHook.showCategory(false);
                }
            }
            case 460 -> shot(mc, "06_jei_separation");
            case 470 -> {
                mc.gui.setScreen(null);
                server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        String.format(java.util.Locale.ROOT, "tp @a %d %d %d 200 22", origin.getX() - 1, origin.getY() + 1, origin.getZ() - 2)));
            }
            case 500 -> { setHudHidden(mc, true); shot(mc, "07_sheep_closeup"); }
            case 505 -> {
                setHudHidden(mc, false);
                mc.gui.setScreen(net.neoforged.neoforge.client.gui.modlist.ModListScreen.create(null));
            }
            case 535 -> shot(mc, "08_mod_list");
            case 545 -> {
                ResourcefulSheep.LOGGER.warn("Screenshot harness finished; client definitions loaded: {}", SheepResourceRegistry.client().size());
                mc.stop();
            }
            default -> {}
        }
    }

    private static void setHudHidden(Minecraft mc, boolean hidden) {
        if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
    }

    private static void command(MinecraftServer server, String cmd) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd));
    }

    private static void look(MinecraftServer server, float yaw, float pitch) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                String.format(java.util.Locale.ROOT, "tp @a %d %d %d %.1f %.1f", origin.getX(), origin.getY(), origin.getZ(), yaw, pitch));
    }

    private static void setupScene(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        ServerLevel level = player.level();
        origin = player.blockPosition();
        for (String c : new String[]{"time set noon", "weather clear", "gamerule send_command_feedback false", "gamerule advance_time false", "gamerule advance_weather false"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), c);
        }
        BlockPos o = origin;
        // Clear a stage in front of the player (north).
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, 0, -10), o.offset(8, 5, 2))) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-8, -1, -10), o.offset(8, -1, 2))) {
            level.setBlock(p, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
        }
        // Infused sheep in a row, side-on to the camera, plus a plain one for comparison.
        Object[][] sheep = {
                {"iron", DyeColor.WHITE, -4}, {"gold", DyeColor.WHITE, -2}, {"diamond", DyeColor.LIGHT_BLUE, 0},
                {"redstone", DyeColor.WHITE, 2}, {null, DyeColor.WHITE, 4}};
        for (Object[] s : sheep) {
            Sheep sh = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sh.snapTo(o.getX() + (int) s[2] + 0.5, o.getY(), o.getZ() - 5 + 0.5, 90, 0);
            sh.setYHeadRot(90);
            sh.setYBodyRot(90);
            sh.setNoAi(true);
            sh.setColor((DyeColor) s[1]);
            if (s[0] != null) {
                SheepInfusionEvents.setInfusion(sh, new SheepInfusion(Optional.of(ResourcefulSheep.id((String) s[0])), 9, 0));
            }
            level.addFreshEntity(sh);
        }
        // Two separators: one idle, one running (facing the camera).
        BlockPos idle = o.offset(-2, 0, -8), active = o.offset(2, 0, -8);
        level.setBlock(idle, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(active, ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.SOUTH), 3);
        level.setBlock(o.offset(0, 0, -8), ModBlocks.MECHANICAL_SEPARATOR.get().defaultBlockState().setValue(MechanicalSeparatorBlock.FACING, Direction.WEST), 3);
        level.setBlock(o.offset(3, 0, -8), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        level.setBlock(o.offset(5, 0, -3), ModBlocks.CREATIVE_GENERATOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(active) instanceof MechanicalSeparatorBlockEntity be) {
            fill(be, 64);
        }
        // Inventory: every wool type plus the machine.
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(ModItems.MECHANICAL_SEPARATOR.get(), 4));
        player.getInventory().add(new ItemStack(ModItems.CREATIVE_GENERATOR.get(), 1));
        SheepResourceRegistry.server().all().forEach(r -> player.getInventory().add(ResourceWoolItem.create(r.id(), 16)));
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 8));
        player.getInventory().add(new ItemStack(Items.SHEARS));
        look(server, 180, 12);
    }

    private static void fill(MechanicalSeparatorBlockEntity be, int wool) {
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 0; i < 200; i++) be.getEnergy().insert(1000, tx);
            tx.commit();
        }
        be.getInventory().setStack(SeparatorInventory.INPUT, ResourceWoolItem.create(ResourcefulSheep.id("iron"), wool));
    }

    private static void openSeparator(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
        BlockPos pos = origin.offset(2, 0, -8);
        if (player.level().getBlockEntity(pos) instanceof MechanicalSeparatorBlockEntity be) {
            player.openMenu(be, pos);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.gameRenderer.mainRenderTarget(), 1,
                msg -> ResourcefulSheep.LOGGER.warn("Harness screenshot {}: {}", name, msg.getString()));
    }
}
