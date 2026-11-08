package com.jax3.resourcefulsheep.recipe;

import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.registry.ModRecipes;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.Separation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Single place that decides what the Mechanical Separator does with an item.
 *
 * <ol>
 *     <li>An explicit {@code resourcefulsheep:separating} recipe matching the item wins (lets packs override).</li>
 *     <li>Otherwise, resource wool uses its sheep resource definition's {@code separator} block.</li>
 * </ol>
 * Works on both logical sides: the client uses recipes received through NeoForge's recipe sync, so slot validation
 * in the GUI matches the server.
 */
public final class SeparationLookup {
    private static volatile RecipeMap clientRecipes = RecipeMap.EMPTY;

    private SeparationLookup() {}

    public static void setClientRecipes(RecipeMap map) {
        clientRecipes = map;
    }

    public static Optional<Separation> find(Level level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        Optional<SeparatingRecipe> recipe = findRecipe(level, stack);
        if (recipe.isPresent()) return recipe.map(SeparatingRecipe::toSeparation);
        return ResourceWoolItem.getResource(stack, level.isClientSide()).map(SheepResource::separation);
    }

    public static boolean canProcess(Level level, ItemStack stack) {
        return find(level, stack).isPresent();
    }

    private static Optional<SeparatingRecipe> findRecipe(Level level, ItemStack stack) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        if (level instanceof ServerLevel server) {
            return server.recipeAccess().getRecipeFor(ModRecipes.SEPARATING_TYPE.get(), input, level).map(RecipeHolder::value);
        }
        return clientRecipes.getRecipesFor(ModRecipes.SEPARATING_TYPE.get(), input, level).findFirst().map(RecipeHolder::value);
    }

    /** Explicit separating recipes known to the client (for JEI). */
    public static List<RecipeHolder<SeparatingRecipe>> clientRecipes() {
        Collection<RecipeHolder<SeparatingRecipe>> recipes = clientRecipes.byType(ModRecipes.SEPARATING_TYPE.get());
        return List.copyOf(recipes);
    }
}
