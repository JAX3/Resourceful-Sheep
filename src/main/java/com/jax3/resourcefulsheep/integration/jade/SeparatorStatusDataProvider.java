package com.jax3.resourcefulsheep.integration.jade;

import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

/** Server half: sends the separator's status, FE/t and redstone mode to the client (Jade asks a few times a second). */
public enum SeparatorStatusDataProvider implements StreamServerDataProvider<BlockAccessor, SeparatorStatusDataProvider.Data> {
    INSTANCE;

    public record Data(int status, int energyPerTick, int redstoneMode) {
        static final StreamCodec<RegistryFriendlyByteBuf, Data> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::status,
                ByteBufCodecs.VAR_INT, Data::energyPerTick,
                ByteBufCodecs.VAR_INT, Data::redstoneMode,
                Data::new);
    }

    @Override
    public @Nullable Data streamData(BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MechanicalSeparatorBlockEntity be)) return null;
        return new Data(be.getStatus().ordinal(), be.getEnergyPerTick(), be.getRedstoneMode().ordinal());
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Data> streamCodec() {
        return Data.CODEC;
    }

    @Override
    public Identifier getUid() {
        return SeparatorStatusProvider.UID;
    }
}
