package com.jax3.resourcefulsheep.client;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.entity.SheepInfusion;
import com.jax3.resourcefulsheep.entity.SheepInfusionEvents;
import com.jax3.resourcefulsheep.recipe.SeparationLookup;
import com.jax3.resourcefulsheep.registry.ModAttachments;
import com.jax3.resourcefulsheep.registry.ModMenus;
import com.jax3.resourcefulsheep.registry.ModTags;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Client-only entry point. Never loaded on a dedicated server. */
@Mod(value = ResourcefulSheep.MOD_ID, dist = Dist.CLIENT)
public final class ResourcefulSheepClient {
    public ResourcefulSheepClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(ResourcefulSheepClient::registerScreens);
        modBus.addListener(ResourcefulSheepClient::registerTintSources);
        modBus.addListener(ResourcefulSheepClient::registerRenderStateModifiers);
        modBus.addListener(ResourcefulSheepClient::addLayers);

        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onRecipesReceived);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ResourcefulSheepClient::onItemTooltip);

        DevScreenshotHarness.registerIfEnabled();
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MECHANICAL_SEPARATOR.get(), MechanicalSeparatorScreen::new);
    }

    private static void registerTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(ResourcefulSheep.id("resource_wool"), ResourceWoolTintSource.MAP_CODEC);
    }

    private static void registerRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.<Sheep, SheepRenderState>registerEntityModifier(SheepRenderer.class, (sheep, state) -> InfusedWoolLayer.extract(sheep, state));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Vanilla sheep plus any modded entity whose renderer extends SheepRenderer.
        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?, ?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof SheepRenderer sheepRenderer) {
                sheepRenderer.addLayer(new InfusedWoolLayer(sheepRenderer, event.getEntityModels()));
            }
        }
    }

    private static void onRecipesReceived(RecipesReceivedEvent event) {
        RecipeMap map = event.getRecipeMap();
        SeparationLookup.setClientRecipes(map);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SheepResourceRegistry.setClient(new SheepResourceRegistry(Map.of()));
        SeparationLookup.setClientRecipes(RecipeMap.EMPTY);
    }

    private static void onItemTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        if (event.getItemStack().is(com.jax3.resourcefulsheep.registry.ModItems.CREATIVE_GENERATOR.get())) {
            event.getToolTip().add(1, net.minecraft.network.chat.Component.translatable("tooltip.resourcefulsheep.creative_generator")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Occasional resource-coloured particles around infused sheep that still have wool. */
    private static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (!entity.hasData(ModAttachments.SHEEP_INFUSION) || !ModTags.isInfusable(entity)) return;
        if (!ModConfigs.getBool(ModConfigs.CLIENT.ambientParticles) || entity.tickCount % 10 != 0) return;
        if (entity.isInvisible() || living.getRandom().nextInt(4) != 0) return;
        if (entity instanceof Sheep sheep && sheep.isSheared()) return;
        SheepInfusion infusion = entity.getData(ModAttachments.SHEEP_INFUSION);
        if (!infusion.isInfused()) return;
        SheepInfusionEvents.getInfusedResource(entity).ifPresent(resource -> entity.level().addParticle(
                new DustParticleOptions(resource.accentColor(), 0.7F),
                entity.getRandomX(0.6), entity.getY() + entity.getBbHeight() * (0.55 + living.getRandom().nextDouble() * 0.4), entity.getRandomZ(0.6),
                0, 0.01, 0));
    }
}
