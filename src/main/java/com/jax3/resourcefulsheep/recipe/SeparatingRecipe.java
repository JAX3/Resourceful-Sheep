package com.jax3.resourcefulsheep.recipe;

import com.jax3.resourcefulsheep.registry.ModRecipes;
import com.jax3.resourcefulsheep.resource.ChanceOutput;
import com.jax3.resourcefulsheep.resource.Separation;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * An explicit Mechanical Separator recipe. Resource wool is handled automatically from sheep resource definitions;
 * these recipes let packs process any other item (or override a wool type) with the same machine.
 *
 * <pre>{@code
 * {
 *   "type": "resourcefulsheep:separating",
 *   "ingredient": "#minecraft:wool",
 *   "primary_output": { "id": "minecraft:string", "count": 3 },
 *   "secondary_output": { "id": "minecraft:white_dye" },          // optional
 *   "bonus_output": { "item": "minecraft:string", "chance": 0.1 }, // optional
 *   "energy_cost": 4000,                                          // optional, FE
 *   "processing_ticks": 40                                        // optional
 * }
 * }</pre>
 */
public final class SeparatingRecipe implements Recipe<SingleRecipeInput> {
    public static final MapCodec<SeparatingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ingredient").forGetter(SeparatingRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("primary_output").forGetter(SeparatingRecipe::primary),
            ItemStackTemplate.CODEC.optionalFieldOf("secondary_output").forGetter(SeparatingRecipe::secondary),
            ChanceOutput.CODEC.optionalFieldOf("bonus_output").forGetter(SeparatingRecipe::bonus),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("energy_cost").forGetter(SeparatingRecipe::energyCost),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_ticks").forGetter(SeparatingRecipe::processingTicks)
    ).apply(i, SeparatingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeparatingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, SeparatingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC, SeparatingRecipe::primary,
            ByteBufCodecs.optional(ItemStackTemplate.STREAM_CODEC), SeparatingRecipe::secondary,
            ByteBufCodecs.optional(ChanceOutput.STREAM_CODEC), SeparatingRecipe::bonus,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), SeparatingRecipe::energyCost,
            ByteBufCodecs.optional(ByteBufCodecs.VAR_INT), SeparatingRecipe::processingTicks,
            SeparatingRecipe::new);

    @SuppressWarnings("deprecation")
    public static final RecipeSerializer<SeparatingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Ingredient ingredient;
    private final ItemStackTemplate primary;
    private final Optional<ItemStackTemplate> secondary;
    private final Optional<ChanceOutput> bonus;
    private final Optional<Integer> energyCost;
    private final Optional<Integer> processingTicks;
    private @Nullable PlacementInfo placementInfo;

    public SeparatingRecipe(Ingredient ingredient, ItemStackTemplate primary, Optional<ItemStackTemplate> secondary,
                            Optional<ChanceOutput> bonus, Optional<Integer> energyCost, Optional<Integer> processingTicks) {
        this.ingredient = ingredient;
        this.primary = primary;
        this.secondary = secondary;
        this.bonus = bonus;
        this.energyCost = energyCost;
        this.processingTicks = processingTicks;
    }

    public Ingredient ingredient() { return ingredient; }
    public ItemStackTemplate primary() { return primary; }
    public Optional<ItemStackTemplate> secondary() { return secondary; }
    public Optional<ChanceOutput> bonus() { return bonus; }
    public Optional<Integer> energyCost() { return energyCost; }
    public Optional<Integer> processingTicks() { return processingTicks; }

    public Separation toSeparation() {
        return new Separation(primary, secondary, bonus, energyCost, processingTicks);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return primary.create();
    }

    @Override
    public boolean isSpecial() {
        return true; // not shown in the vanilla recipe book
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<SeparatingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<SeparatingRecipe> getType() {
        return ModRecipes.SEPARATING_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) placementInfo = PlacementInfo.create(ingredient);
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return ModRecipes.SEPARATING_CATEGORY.get();
    }
}
