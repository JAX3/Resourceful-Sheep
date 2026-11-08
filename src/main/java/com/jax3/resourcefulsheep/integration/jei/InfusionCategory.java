package com.jax3.resourcefulsheep.integration.jei;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.resource.SheepResource;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
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
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** "Resource Infusion": feeding item + sheep -> resource wool. */
public class InfusionCategory extends AbstractRecipeCategory<InfusionDisplay> {
    public static final IRecipeType<InfusionDisplay> TYPE = IRecipeType.create(ResourcefulSheep.id("resource_infusion"), InfusionDisplay.class);
    public static final int WIDTH = 150, HEIGHT = 58;

    private final IDrawable sheep;

    public InfusionCategory(IGuiHelper gui) {
        super(TYPE, Component.translatable("jei.resourcefulsheep.resource_infusion"),
                gui.drawableBuilder(JeiTextures.TEXTURE, 0, 0, 16, 16).setTextureSize(256, 256).build(), WIDTH, HEIGHT);
        this.sheep = gui.drawableBuilder(JeiTextures.TEXTURE, 0, 16, 32, 24).setTextureSize(256, 256).build();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionDisplay recipe, IFocusGroup focuses) {
        SheepResource r = recipe.resource();
        int amount = r.effectiveFeedingAmount();
        List<ItemStack> feeding = r.feedingItems().stream().map(h -> new ItemStack(h, amount)).toList();
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 8)
                .setStandardSlotBackground()
                .addItemStacks(feeding)
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.resourcefulsheep.consumed", amount).withStyle(ChatFormatting.GRAY)));
        int drop = Math.max(1, r.effectiveWoolDropCount());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 124, 8)
                .setOutputSlotBackground()
                .add(ResourceWoolItem.create(r.id(), drop))
                .addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("jei.resourcefulsheep.per_shearing", drop).withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InfusionDisplay recipe, IFocusGroup focuses) {
        builder.addRecipePlusSignWidget().setPosition(27, 9);
        builder.addDrawableWidget(sheep).setPosition(42, 4);
        builder.addRecipeArrowWidget().setPosition(87, 8);
        SheepResource r = recipe.resource();
        int uses = r.effectiveInfusionUses();
        Component usesLine = uses < 0
                ? Component.translatable("jei.resourcefulsheep.uses_permanent")
                : Component.translatable("jei.resourcefulsheep.uses", uses);
        builder.addText(List.<FormattedText>of(usesLine, Component.translatable("jei.resourcefulsheep.category", r.category().displayName())), WIDTH, 22)
                .setPosition(0, 34)
                .setColor(0xFF555555)
                .setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
    }

    @Override
    public @Nullable Identifier getIdentifier(InfusionDisplay recipe) {
        return recipe.resource().id();
    }
}
