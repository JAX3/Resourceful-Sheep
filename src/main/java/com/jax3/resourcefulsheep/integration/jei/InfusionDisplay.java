package com.jax3.resourcefulsheep.integration.jei;

import com.jax3.resourcefulsheep.resource.SheepResource;

/** JEI recipe: feed {@code resource}'s feeding item to a sheep, shear it for resource wool. */
public record InfusionDisplay(SheepResource resource) {}
