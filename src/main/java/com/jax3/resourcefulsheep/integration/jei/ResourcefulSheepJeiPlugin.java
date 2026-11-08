package com.jax3.resourcefulsheep.integration.jei;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.items.ResourceWoolItem;
import com.jax3.resourcefulsheep.recipe.SeparatingRecipe;
import com.jax3.resourcefulsheep.recipe.SeparationLookup;
import com.jax3.resourcefulsheep.registry.ModDataComponents;
import com.jax3.resourcefulsheep.registry.ModItems;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * JEI integration. Only loaded by JEI (via {@link JeiPlugin}), so the mod runs fine without JEI installed.
 *
 * <p>Recipes are built from the client copy of the sheep resource definitions (synced before JEI starts) and the
 * {@code resourcefulsheep:separating} recipes received from the server, i.e. exactly the data the machine uses.</p>
 */
@JeiPlugin
public class ResourcefulSheepJeiPlugin implements IModPlugin {
    private static final Identifier UID = ResourcefulSheep.id("jei_plugin");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        // Each resource wool variant is a distinct JEI ingredient.
        registration.registerFromDataComponentTypes(ModItems.RESOURCE_WOOL.get(), ModDataComponents.SHEEP_RESOURCE.get());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new InfusionCategory(gui), new SeparationCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        SheepResourceRegistry resources = SheepResourceRegistry.client();
        registration.addRecipes(InfusionCategory.TYPE, resources.all().stream().map(InfusionDisplay::new).toList());
        registration.addRecipes(SeparationCategory.TYPE, buildSeparations(resources));

        List<ItemStack> wool = resources.all().stream().map(r -> ResourceWoolItem.create(r.id(), 1)).toList();
        if (!wool.isEmpty()) {
            registration.addItemStackInfo(wool, Component.translatable("jei.resourcefulsheep.info.resource_wool"));
        }
        registration.addItemStackInfo(new ItemStack(ModItems.MECHANICAL_SEPARATOR.get()), Component.translatable("jei.resourcefulsheep.info.separator"));
    }

    static List<SeparationDisplay> buildSeparations(SheepResourceRegistry resources) {
        List<SeparationDisplay> displays = new ArrayList<>();
        List<RecipeHolder<SeparatingRecipe>> recipes = SeparationLookup.clientRecipes();
        for (RecipeHolder<SeparatingRecipe> holder : recipes) {
            List<ItemStack> inputs = Arrays.stream(holder.value().ingredient().items().map(ItemStack::new).toArray(ItemStack[]::new)).toList();
            displays.add(new SeparationDisplay(holder.id().identifier(), inputs, holder.value().toSeparation()));
        }
        for (SheepResource resource : resources.all()) {
            ItemStack wool = ResourceWoolItem.create(resource.id(), 1);
            // An explicit recipe that matches this wool overrides the definition in the machine; show only that one.
            boolean overridden = recipes.stream().anyMatch(h -> h.value().ingredient().test(wool));
            if (!overridden) {
                displays.add(new SeparationDisplay(resource.id(), List.of(wool), resource.separation()));
            }
        }
        return displays;
    }

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        JeiDevHook.runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiDevHook.runtime = null;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(SeparationCategory.TYPE, ModItems.MECHANICAL_SEPARATOR.get());
        registration.addCraftingStation(InfusionCategory.TYPE, Items.SHEARS);
    }
}
