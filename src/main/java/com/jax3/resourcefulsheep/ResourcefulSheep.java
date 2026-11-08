package com.jax3.resourcefulsheep;

import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.entity.SheepInfusionEvents;
import com.jax3.resourcefulsheep.gametest.ModGameTests;
import com.jax3.resourcefulsheep.network.ModNetwork;
import com.jax3.resourcefulsheep.registry.ModAttachments;
import com.jax3.resourcefulsheep.registry.ModBlockEntities;
import com.jax3.resourcefulsheep.registry.ModBlocks;
import com.jax3.resourcefulsheep.registry.ModCapabilities;
import com.jax3.resourcefulsheep.registry.ModCreativeTabs;
import com.jax3.resourcefulsheep.registry.ModDataComponents;
import com.jax3.resourcefulsheep.registry.ModItems;
import com.jax3.resourcefulsheep.registry.ModLootModifiers;
import com.jax3.resourcefulsheep.registry.ModMenus;
import com.jax3.resourcefulsheep.registry.ModRecipes;
import com.jax3.resourcefulsheep.resource.SheepResourceManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Entry point for Resourceful Sheep.
 *
 * <p>Everything gameplay related is registered from here on the mod event bus; game-bus listeners live in
 * {@link SheepInfusionEvents} and {@link SheepResourceManager}. Client-only code is registered from
 * {@code ResourcefulSheepClient}, which is a separate {@code @Mod(dist = CLIENT)} entry point so that dedicated
 * servers never load client classes.</p>
 */
@Mod(ResourcefulSheep.MOD_ID)
public final class ResourcefulSheep {
    public static final String MOD_ID = "resourcefulsheep";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ResourcefulSheep(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModAttachments.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModMenus.REGISTER.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModRecipes.BOOK_CATEGORIES.register(modBus);
        ModLootModifiers.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);

        modBus.addListener(ModCapabilities::register);
        modBus.addListener(ModNetwork::register);

        // Game tests (only active when the game test framework is enabled, e.g. ./gradlew runGameTestServer).
        ModGameTests.FUNCTIONS.register(modBus);
        modBus.addListener(ModGameTests::registerTests);

        container.registerConfig(ModConfig.Type.SYNCED, ModConfigs.SERVER_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ModConfigs.CLIENT_SPEC);

        NeoForge.EVENT_BUS.register(SheepResourceManager.Events.class);
        NeoForge.EVENT_BUS.register(SheepInfusionEvents.class);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
