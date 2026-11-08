package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.MachineContents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ResourcefulSheep.MOD_ID);

    /** Which sheep resource a piece of {@code resourcefulsheep:resource_wool} contains. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Identifier>> SHEEP_RESOURCE = REGISTER.registerComponentType(
            "sheep_resource", b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC).cacheEncoding());

    /** Stored energy/redstone mode kept on a broken Mechanical Separator item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineContents>> MACHINE_CONTENTS = REGISTER.registerComponentType(
            "machine_contents", b -> b.persistent(MachineContents.CODEC).networkSynchronized(MachineContents.STREAM_CODEC));

    private ModDataComponents() {}
}
