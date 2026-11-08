package com.jax3.resourcefulsheep.registry;

import com.jax3.resourcefulsheep.ResourcefulSheep;
import com.jax3.resourcefulsheep.blocks.CreativeGeneratorBlock;
import com.jax3.resourcefulsheep.blocks.MechanicalSeparatorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(ResourcefulSheep.MOD_ID);

    /** Iron-tier machine: needs a stone pickaxe or better, hardness 3.5 (like a furnace), blast resistance 6. */
    public static final DeferredBlock<MechanicalSeparatorBlock> MECHANICAL_SEPARATOR = REGISTER.registerBlock("mechanical_separator",
            MechanicalSeparatorBlock::new,
            p -> p.mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(state -> state.getValue(MechanicalSeparatorBlock.ACTIVE) ? 7 : 0));

    /** Creative-only infinite FE source: unbreakable in survival, no recipe. */
    public static final DeferredBlock<CreativeGeneratorBlock> CREATIVE_GENERATOR = REGISTER.registerBlock("creative_generator",
            CreativeGeneratorBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noLootTable()
                    .sound(SoundType.METAL)
                    .lightLevel(state -> 10));

    private ModBlocks() {}
}
