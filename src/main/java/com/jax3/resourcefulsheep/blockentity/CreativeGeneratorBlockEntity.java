package com.jax3.resourcefulsheep.blockentity;

import com.jax3.resourcefulsheep.config.ModConfigs;
import com.jax3.resourcefulsheep.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.InfiniteEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Pushes up to {@code creativeGeneratorOutput} FE per tick into each adjacent energy receiver. */
public class CreativeGeneratorBlockEntity extends BlockEntity {
    private long lastTickPushed;

    public CreativeGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_GENERATOR.get(), pos, state);
    }

    public void serverTick(ServerLevel level, BlockPos pos) {
        int rate = ModConfigs.getInt(ModConfigs.SERVER.creativeGeneratorOutput);
        if (rate <= 0) return;
        long pushed = 0;
        for (Direction dir : Direction.values()) {
            BlockPos target = pos.relative(dir);
            if (level.getBlockEntity(target) instanceof CreativeGeneratorBlockEntity) continue; // avoid generator loops
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, target, dir.getOpposite());
            if (handler == null) continue;
            try (Transaction tx = Transaction.openRoot()) {
                pushed += handler.insert(rate, tx);
                tx.commit();
            }
        }
        lastTickPushed = pushed;
    }

    /** FE pushed to neighbours during the last tick (used by tests and for debugging). */
    public long getLastTickPushed() {
        return lastTickPushed;
    }

    /** Every side exposes an infinite source, so pull-based cables work too. */
    public EnergyHandler getEnergyHandler() {
        return InfiniteEnergyHandler.INSTANCE;
    }
}
