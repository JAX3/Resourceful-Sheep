package com.jax3.resourcefulsheep.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Energy and redstone mode carried by a Mechanical Separator item after it is broken. */
public record MachineContents(int energy, RedstoneMode redstoneMode) {
    public static final Codec<MachineContents> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("energy", 0).forGetter(MachineContents::energy),
            Codec.INT.xmap(RedstoneMode::byId, RedstoneMode::ordinal).optionalFieldOf("redstone_mode", RedstoneMode.IGNORED).forGetter(MachineContents::redstoneMode)
    ).apply(i, MachineContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MachineContents::energy,
            ByteBufCodecs.VAR_INT.map(RedstoneMode::byId, RedstoneMode::ordinal), MachineContents::redstoneMode,
            MachineContents::new);
}
