package com.jax3.resourcefulsheep.blocks;

import com.jax3.resourcefulsheep.blockentity.CreativeGeneratorBlockEntity;
import com.jax3.resourcefulsheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Creative-only infinite Forge Energy source for testing. Every tick it pushes FE into all adjacent blocks that
 * accept energy, and cables or machines may also pull unlimited FE from it on any side. It has no recipe.
 */
public class CreativeGeneratorBlock extends BaseEntityBlock {
    public CreativeGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeGeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel serverLevel)) return null;
        return createTickerHelper(type, ModBlockEntities.CREATIVE_GENERATOR.get(),
                (l, pos, s, be) -> be.serverTick(serverLevel, pos));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        double x = pos.getX() + random.nextDouble(), y = pos.getY() + 1.02, z = pos.getZ() + random.nextDouble();
        level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0xFF6AE6 : 0x9A52F0, 0.8F), x, y, z, 0, 0.03, 0);
    }
}
