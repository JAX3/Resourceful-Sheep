package com.jax3.resourcefulsheep.integration.jei;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.registry.ModItems;
import com.jax3.resourcefulsheep.resource.Separation;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** "Mechanical Separation": wool -> resource + string (+ chance bonus), with FE and time. */
public class SeparationCategory extends AbstractRecipeCategory<SeparationDisplay> {
    public static final IRecipeType<SeparationDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("mechanical_separation"), SeparationDisplay.class);
    public static final int WIDTH = 150, HEIGHT = 64;

    private final IDrawable energyEmpty;
    private final IDrawableAnimated energyFull;

    public SeparationCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.resourcefulsheep.mechanical_separation"),
                gui.createDrawableItemLike(ModItems.MECHANICAL_SEPARATOR.get()), WIDTH, HEIGHT);
        this.energyEmpty = gui.drawableBuilder(JeiTextures.TEXTURE, 32, 16, 8, 40).setTextureSize(256, 256).build();
        this.energyFull = gui.drawableBuilder(JeiTextures.TEXTURE, 40, 16, 8, 40).setTextureSize(256, 256)
                .buildAnimated(100, IDrawableAnimated.StartDirection.TOP, true);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SeparationDisplay recipe, IFocusGroup focuses) {
        Separation s = recipe.separation();
        builder.addSlot(RecipeIngredientRole.INPUT, 20, 12)
                .setStandardSlotBackground()
                .addItemStacks(recipe.inputs());

        double expected = s.expectedPrimaryCount();
        builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 3)
                .setOutputSlotBackground()
                .add(s.primary().withCount(Math.max(1, (int) Math.floor(expected))))
                .addRichTooltipCallback((view, tooltip) -> {
                    if (expected != Math.floor(expected)) {
                        tooltip.add(Component.translatable("jei.resourcefulsheep.average", String.format(Locale.ROOT, "%.2f", expected)).withStyle(ChatFormatting.GRAY));
                    }
                });
        s.secondary().ifPresent(sec -> builder.addSlot(RecipeIngredientRole.OUTPUT, 82, 25)
                .setStandardSlotBackground()
                .add(sec));
        s.bonus().ifPresent(bonus -> builder.addSlot(RecipeIngredientRole.OUTPUT, 106, 3)
                .setStandardSlotBackground()
                .add(bonus.item())
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.resourcefulsheep.chance",
                        Math.round(bonus.chance() * 100)).withStyle(ChatFormatting.GOLD))));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, SeparationDisplay recipe, IFocusGroup focuses) {
        Separation s = recipe.separation();
        builder.addDrawableWidget(energyEmpty).setPosition(4, 4);
        builder.addDrawableWidget(energyFull).setPosition(4, 4);
        builder.addTooltipArea(4, 4, 8, 40).setTooltip(List.<FormattedText>of(
                Component.translatable("jei.resourcefulsheep.energy", fe(s.effectiveEnergy())),
                Component.translatable("jei.resourcefulsheep.energy_per_tick", fe(s.energyPerTick())).withStyle(ChatFormatting.GRAY)));
        builder.addAnimatedRecipeArrowWidget(s.effectiveTicks()).setPosition(48, 12);
        builder.addText(List.<FormattedText>of(
                        Component.translatable("jei.resourcefulsheep.energy", fe(s.effectiveEnergy())),
                        Component.translatable("jei.resourcefulsheep.time", String.format(Locale.ROOT, "%.1f", s.effectiveTicks() / 20.0))),
                        WIDTH - 16, 20)
                .setPosition(16, 45)
                .setColor(0xFF555555)
                .setTextAlignment(HorizontalAlignment.LEFT);
    }

    @Override
    public @Nullable Identifier getIdentifier(SeparationDisplay recipe) {
        return recipe.id();
    }

    private static String fe(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
