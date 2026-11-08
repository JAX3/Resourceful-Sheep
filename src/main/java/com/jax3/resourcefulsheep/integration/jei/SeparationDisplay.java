package com.jax3.resourcefulsheep.integration.jei;

import com.jax3.resourcefulsheep.resource.Separation;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * JEI recipe for the Mechanical Separator, built from either a sheep resource definition or an explicit
 * {@code resourcefulsheep:separating} recipe (both resolve to the same {@link Separation} the machine uses).
 */
public record SeparationDisplay(Identifier id, List<ItemStack> inputs, Separation separation) {}
