package com.jax3.resourcefulsheep.resource;

import com.jax3.resourcefulsheep.config.ModConfigs;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStackTemplate;

/**
 * A fully resolved Mechanical Separator operation, regardless of whether it came from a sheep resource definition or
 * an explicit {@code resourcefulsheep:separating} recipe. Machine logic and JEI both read from this type.
 *
 * <p>Energy and time are stored "raw" (as written in data, empty meaning "use the config default"); the
 * {@code effective*} methods apply the global config so every consumer sees identical numbers.</p>
 */
public record Separation(
        ItemStackTemplate primary,
        Optional<ItemStackTemplate> secondary,
        Optional<ChanceOutput> bonus,
        Optional<Integer> energyCost,
        Optional<Integer> processingTicks) {

    /** Processing duration in ticks after the speed multiplier. Always at least 1. */
    public int effectiveTicks() {
        int base = processingTicks.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultProcessingTicks));
        double speed = ModConfigs.getDouble(ModConfigs.SERVER.processingSpeedMultiplier);
        return Math.max(1, Mth.ceil(base / speed));
    }

    /** FE consumed per processing tick. */
    public int energyPerTick() {
        int fixed = ModConfigs.getInt(ModConfigs.SERVER.energyPerTickOverride);
        if (fixed > 0) return fixed;
        long total = (long) Math.ceil(energyCost.orElseGet(() -> ModConfigs.getInt(ModConfigs.SERVER.defaultEnergyCost))
                * ModConfigs.getDouble(ModConfigs.SERVER.energyCostMultiplier));
        return (int) Math.min(Integer.MAX_VALUE, (total + effectiveTicks() - 1) / effectiveTicks());
    }

    /** Total FE for one operation (per-tick usage x ticks, so it is exactly what the machine draws). */
    public long effectiveEnergy() {
        return (long) energyPerTick() * effectiveTicks();
    }

    /** Average primary output count after the global multiplier (shown in JEI and tooltips). */
    public double expectedPrimaryCount() {
        return primary.count() * ModConfigs.getDouble(ModConfigs.SERVER.primaryOutputMultiplier);
    }

    /** Maximum number of primary items one operation can produce (used for output-space checks). */
    public int maxPrimaryCount() {
        return Mth.ceil(expectedPrimaryCount()) + (ModConfigs.getDouble(ModConfigs.SERVER.bonusPrimaryChance) > 0 ? 1 : 0);
    }
}
