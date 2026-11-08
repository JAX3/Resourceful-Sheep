package com.jax3.resourcefulsheep.resource;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.network.SyncSheepResourcesPayload;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Loads sheep resource definitions from {@code data/<namespace>/resourcefulsheep/sheep_resources/**.json}.
 *
 * <p>Definitions are reloaded with {@code /reload}. Files that reference items from mods that are not installed are
 * skipped with a single warning instead of failing the whole reload, and files may use NeoForge load conditions
 * ({@code "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "mekanism"}]}).</p>
 */
public final class SheepResourceManager extends SimplePreparableReloadListener<Map<Identifier, JsonElement>> {
    public static final Identifier ID = ResourcefulSheep.id("sheep_resources");
    public static final FileToIdConverter LISTER = FileToIdConverter.json("resourcefulsheep/sheep_resources");

    @Override
    protected Map<Identifier, JsonElement> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> raw = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : LISTER.listMatchingResources(manager).entrySet()) {
            Identifier id = LISTER.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                raw.put(id, StrictJsonParser.parse(reader));
            } catch (IOException | JsonParseException | IllegalArgumentException e) {
                ResourcefulSheep.LOGGER.error("Sheep resource '{}' is not valid JSON ({}): {}", id, entry.getKey(), e.getMessage());
            }
        }
        return raw;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> raw, ResourceManager manager, ProfilerFiller profiler) {
        DynamicOps<JsonElement> ops = makeConditionalOps();
        var codec = ConditionalOps.createConditionalCodec(SheepResource.DEFINITION_CODEC);
        Map<Identifier, SheepResource> loaded = new HashMap<>();
        int skipped = 0;
        for (Map.Entry<Identifier, JsonElement> entry : raw.entrySet()) {
            Identifier id = entry.getKey();
            DataResult<Optional<SheepResource.Definition>> result = codec.parse(ops, entry.getValue());
            if (result.error().isPresent()) {
                String message = result.error().get().message();
                if (message.contains("Unknown registry key") || message.contains("Unknown element name")) {
                    ResourcefulSheep.LOGGER.warn("Skipping sheep resource '{}': it references an item that does not exist (is the providing mod installed?). {}", id, message);
                } else {
                    ResourcefulSheep.LOGGER.error("Skipping invalid sheep resource '{}': {}", id, message);
                }
                skipped++;
                continue;
            }
            Optional<SheepResource.Definition> definition = result.getOrThrow();
            if (definition.isEmpty()) {
                ResourcefulSheep.LOGGER.debug("Sheep resource '{}' disabled by its load conditions", id);
                continue;
            }
            SheepResource resource = SheepResource.withId(id, definition.get());
            // Tag contents are not bound yet during a reload, so empty tags are reported later (onTagsUpdated).
            loaded.put(id, resource);
        }
        SheepResourceRegistry.setServer(new SheepResourceRegistry(loaded));
        ResourcefulSheep.LOGGER.info("Loaded {} sheep resource definitions ({} skipped)", loaded.size(), skipped);
    }

    /** Game-bus listeners: register the reload listener and keep clients in sync. */
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void addReloadListener(AddServerReloadListenersEvent event) {
            event.addListener(ID, new SheepResourceManager());
        }

        /**
         * Fired when a player joins and after {@code /reload}. Sending here guarantees clients (and JEI, which starts
         * once recipes arrive right after this event) always see the current definitions.
         */
        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            var payload = new SyncSheepResourcesPayload(java.util.List.copyOf(SheepResourceRegistry.server().all()));
            event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
            event.sendRecipes(com.jax3.resourcefulsheep.registry.ModRecipes.SEPARATING_TYPE.get());
        }

        /** Runs once the reload has bound tags: warn about definitions whose feeding tag is empty. */
        @SubscribeEvent
        public static void onTagsUpdated(net.neoforged.neoforge.event.TagsUpdatedEvent.ServerDataLoad event) {
            for (SheepResource resource : SheepResourceRegistry.server().all()) {
                if (resource.feedingItem().isEmpty()) {
                    ResourcefulSheep.LOGGER.warn("Sheep resource '{}' has a feeding_item that matches no items (empty tag?); it can never be fed", resource.id());
                }
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            SheepResourceRegistry.setServer(new SheepResourceRegistry(Map.of()));
        }
    }
}
