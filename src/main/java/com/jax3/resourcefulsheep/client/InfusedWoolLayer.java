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
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

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

    /**
     * What to draw: wool colours ({@code accent} is 0 when the sparkle overlay is disabled), the resource's block
     * growths and how many of them are showing.
     */
    public record Colors(int wool, int accent, ItemStackRenderState growth, int growthCount) {}

    /**
     * Where resource growths sprout on the fleece, in sheep model pixels (x across, z along the body), with a size,
     * a yaw and a lean, ordered so the first ones stay while the infusion runs down.
     */
    private static final float[][] GROWTH_SPOTS = {
            // x,    z,    scale, yaw,  lean
            {-2.2F, -4.5F, 0.58F, 20F, 10F},
            {2.4F, 2.5F, 0.54F, -35F, -12F},
            {1.6F, -6.8F, 0.42F, 55F, -8F},
            {-2.6F, 4.6F, 0.44F, 10F, 14F},
            {0.2F, -0.8F, 0.38F, 70F, 6F},
            {3.6F, -2.6F, 0.32F, -15F, -20F},
    };
    /** Top of the fleece in model pixels (body top 6 minus the fur's 1.75 inflation), sunk slightly into the wool. */
    private static final float FLEECE_TOP = 4.25F + 0.3F;

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

        ItemStackRenderState growth = new ItemStackRenderState();
        int count = 0;
        if (ModConfigs.getBool(ModConfigs.CLIENT.showResourceGrowths)) {
            ItemStack stack = resource.get().growthStack();
            if (!stack.isEmpty()) {
                Minecraft.getInstance().getItemModelResolver().updateForTopItem(growth, stack, ItemDisplayContext.FIXED, sheep.level(), sheep, sheep.getId());
                // More growths while the infusion is fresh; they dwindle as it is sheared away.
                int uses = SheepInfusionEvents.getInfusion(sheep).usesRemaining();
                int max = resource.get().effectiveInfusionUses();
                count = uses < 0 || max <= 0 ? GROWTH_SPOTS.length
                        : Mth.clamp(Mth.ceil(GROWTH_SPOTS.length * (float) uses / max), 2, GROWTH_SPOTS.length);
            }
        }
        state.setRenderData(INFUSED_WOOL, new Colors(wool, accent, growth, count));
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
        if (state.isInvisible || colors.growthCount() <= 0 || colors.growth().isEmpty()) return;

        // Little blocks of the resource sprouting from the fleece. Model space has +Y pointing down, so items are
        // flipped upright with a negative scale (as vanilla does for items worn on heads).
        for (int i = 0; i < colors.growthCount(); i++) {
            float[] spot = GROWTH_SPOTS[i];
            float scale = spot[2];
            poseStack.pushPose();
            getParentModel().root().translateAndRotate(poseStack);
            poseStack.translate(spot[0] / 16.0F, FLEECE_TOP / 16.0F - scale * 0.22F, (spot[1] + 2.0F) / 16.0F);
            poseStack.rotateDegrees(Axis.YP, spot[3]);
            poseStack.rotateDegrees(Axis.ZP, spot[4]);
            poseStack.scale(scale, -scale, -scale);
            colors.growth().submit(poseStack, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
