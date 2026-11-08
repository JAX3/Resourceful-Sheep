package com.jax3.resourcefulsheep.network;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.resource.SheepResource;
import com.jax3.resourcefulsheep.resource.SheepResourceRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: the full set of sheep resource definitions (sent on join and after {@code /reload}). */
public record SyncSheepResourcesPayload(List<SheepResource> resources) implements CustomPacketPayload {
    public static final Type<SyncSheepResourcesPayload> TYPE = new Type<>(ResourcefulSheep.id("sync_sheep_resources"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSheepResourcesPayload> STREAM_CODEC = StreamCodec.composite(
            SheepResource.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncSheepResourcesPayload::resources,
            SyncSheepResourcesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Map<Identifier, SheepResource> map = new HashMap<>();
        for (SheepResource r : resources) map.put(r.id(), r);
        SheepResourceRegistry.setClient(new SheepResourceRegistry(map));
    }
}
