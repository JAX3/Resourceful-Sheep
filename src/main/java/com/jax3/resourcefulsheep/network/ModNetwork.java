package com.jax3.resourcefulsheep.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        // Handled on the main thread (default), since it replaces state read by rendering and JEI.
        registrar.playToClient(SyncSheepResourcesPayload.TYPE, SyncSheepResourcesPayload.STREAM_CODEC, SyncSheepResourcesPayload::handle);
    }
}
