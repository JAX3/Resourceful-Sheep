package com.jax3.resourcefulsheep.blockentity;

import com.jax3.resourcefulsheep.config.ModConfigs;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * FE buffer of the separator, exposed through NeoForge's {@code Capabilities.Energy.BLOCK} (the transfer-API
 * {@code EnergyHandler}). Accepts energy from any FE source/cable; never outputs energy.
 *
 * <p>Capacity and insert rate follow the server config and are refreshed every tick, so changing the config takes
 * effect immediately (excess energy above a reduced capacity is clamped).</p>
 */
public class SeparatorEnergyStorage extends SimpleEnergyHandler {
    private final Runnable onChanged;

    public SeparatorEnergyStorage(Runnable onChanged) {
        super(ModConfigs.getInt(ModConfigs.SERVER.energyCapacity), ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput), 0);
        this.onChanged = onChanged;
    }

    public void refreshLimits() {
        this.capacity = ModConfigs.getInt(ModConfigs.SERVER.energyCapacity);
        this.maxInsert = ModConfigs.getInt(ModConfigs.SERVER.maxEnergyInput);
        if (this.energy > this.capacity) set(this.capacity);
    }

    public int getEnergy() {
        return energy;
    }

    /** Machine-internal consumption (bypasses maxExtract, which is 0 for external callers). */
    public boolean consume(int amount) {
        if (amount > energy) return false;
        set(energy - amount);
        return true;
    }

    @Override
    protected void onEnergyChanged(int previousAmount) {
        onChanged.run();
    }
}
