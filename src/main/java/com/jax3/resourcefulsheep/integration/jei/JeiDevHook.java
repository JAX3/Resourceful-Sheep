package com.jax3.resourcefulsheep.integration.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Used only by the dev screenshot harness to open JEI recipe pages. Never class-loaded unless JEI is installed. */
public final class JeiDevHook {
    static @Nullable IJeiRuntime runtime;

    private JeiDevHook() {}

    /** Shows the recipes producing ({@code output = true}) or using {@code stack}. Returns false if JEI is not ready. */
    public static boolean show(ItemStack stack, boolean output) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        var focus = rt.getJeiHelpers().getFocusFactory().createFocus(
                output ? RecipeIngredientRole.OUTPUT : RecipeIngredientRole.INPUT, VanillaTypes.ITEM_STACK, stack);
        rt.getRecipesGui().show(focus);
        return true;
    }

    /** Opens the Resource Infusion ({@code infusion = true}) or Mechanical Separation category. */
    public static boolean showCategory(boolean infusion) {
        IJeiRuntime rt = runtime;
        if (rt == null) return false;
        rt.getRecipesGui().showTypes(java.util.List.of(infusion ? InfusionCategory.TYPE : SeparationCategory.TYPE));
        return true;
    }
}
