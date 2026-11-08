package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.entity.SheepInfusion;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ResourcefulSheep.MOD_ID);

    /** Infusion state of a sheep. Persisted with the entity (only when non-default) and synced to tracking clients. */
    public static final Supplier<AttachmentType<SheepInfusion>> SHEEP_INFUSION = REGISTER.register("sheep_infusion",
            () -> AttachmentType.builder(() -> SheepInfusion.NONE)
                    .serialize(SheepInfusion.CODEC, infusion -> !infusion.equals(SheepInfusion.NONE))
                    .sync(SheepInfusion.STREAM_CODEC)
                    .build());

    private ModAttachments() {}
}
