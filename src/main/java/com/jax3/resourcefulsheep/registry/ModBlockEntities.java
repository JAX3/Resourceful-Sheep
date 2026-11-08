package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blockentity.CreativeGeneratorBlockEntity;
import com.jax3.resourcefulsheep.blockentity.MechanicalSeparatorBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ResourcefulSheep.MOD_ID);

    public static final Supplier<BlockEntityType<MechanicalSeparatorBlockEntity>> MECHANICAL_SEPARATOR = REGISTER.register("mechanical_separator",
            () -> new BlockEntityType<>(MechanicalSeparatorBlockEntity::new, ModBlocks.MECHANICAL_SEPARATOR.get()));

    public static final Supplier<BlockEntityType<CreativeGeneratorBlockEntity>> CREATIVE_GENERATOR = REGISTER.register("creative_generator",
            () -> new BlockEntityType<>(CreativeGeneratorBlockEntity::new, ModBlocks.CREATIVE_GENERATOR.get()));

    private ModBlockEntities() {}
}
