package com.jax3.resourcefulsheep.resource;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * An output that is only produced with the given probability.
 *
 * <pre>{@code "bonus_output": { "item": { "id": "minecraft:iron_nugget", "count": 3 }, "chance": 0.25 }}</pre>
 */
public record ChanceOutput(ItemStackTemplate item, float chance) {
    public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStackTemplate.CODEC.fieldOf("item").forGetter(ChanceOutput::item),
            ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(ChanceOutput::chance)
    ).apply(i, ChanceOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, ChanceOutput::item,
            ByteBufCodecs.FLOAT, ChanceOutput::chance,
            ChanceOutput::new);
}
