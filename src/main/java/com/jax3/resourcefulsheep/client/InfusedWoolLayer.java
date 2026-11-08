package com.jax3.resourcefulsheep.client;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.entity.SheepInfusionEvents;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.animal.sheep.Sheep;

/**
 * Renders resource-infused wool on sheep.
 *
 * <p>A render-state modifier ({@link #extract}) runs after vanilla extraction: for infused sheep that have wool it
 * stores the colours in the render state and marks the state as sheared so the vanilla wool layer is skipped. This
 * layer then draws the same vanilla fur model (so every animation still lines up) with a greyscale "resource wool"
 * texture tinted with the resource colour, plus a glinting accent overlay. Any resource, including ones added by
 * data packs, is therefore rendered without needing its own texture.</p>
 */
public class InfusedWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    public static final ContextKey<Colors> INFUSED_WOOL = new ContextKey<>(ResourcefulSheep.id("infused_wool"));
    private static final Identifier WOOL_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/infused_wool.png");
    private static final Identifier ACCENT_TEXTURE = ResourcefulSheep.id("textures/entity/sheep/infused_wool_accent.png");

    private final EntityModel<SheepRenderState> model;

    public InfusedWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet models) {
        super(renderer);
        this.model = new SheepFurModel(models.bakeLayer(ModelLayers.SHEEP_WOOL));
    }

    /** Colours to draw with. {@code accent} is 0 when the sparkle overlay is disabled. */
    public record Colors(int wool, int accent) {}

    static void extract(Sheep sheep, SheepRenderState state) {
        if (state.isSheared || state.isBaby) return;
        Optional<SheepResource> resource = SheepInfusionEvents.getInfusedResource(sheep);
        if (resource.isEmpty()) return;
        int wool = ARGB.opaque(resource.get().woolColor());
        if (ModConfigs.getBool(ModConfigs.SERVER.preserveWoolColour)) {
            float strength = (float) ModConfigs.getDouble(ModConfigs.CLIENT.dyeTintStrength);
            wool = ARGB.srgbLerp(strength, wool, ARGB.opaque(state.getWoolColor()));
        }
        int accent = ModConfigs.getBool(ModConfigs.CLIENT.showSheepSparkleOverlay) ? ARGB.opaque(resource.get().accentColor()) : 0;
        state.setRenderData(INFUSED_WOOL, new Colors(wool, accent));
        state.isSheared = true; // hides the vanilla wool layer; the undercoat layer is unaffected
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, SheepRenderState state, float yRot, float xRot) {
        Colors colors = state.getRenderData(INFUSED_WOOL);
        if (colors == null) return;
        coloredCutoutModelCopyLayerRender(model, WOOL_TEXTURE, poseStack, collector, lightCoords, state, colors.wool(), 1);
        if (colors.accent() != 0) {
            coloredCutoutModelCopyLayerRender(model, ACCENT_TEXTURE, poseStack, collector, lightCoords, state, colors.accent(), 2);
        }
    }
}
