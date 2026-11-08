package com.jax3.resourcefulsheep.blockentity;

import net.minecraft.network.chat.Component;

/** How a Mechanical Separator reacts to redstone power. */
public enum RedstoneMode {
    /** Always runs. */
    IGNORED,
    /** Runs only while powered. */
    HIGH,
    /** Runs only while unpowered. */
    LOW,
    /** Never runs. */
    DISABLED;

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> powered;
            case LOW -> !powered;
            case DISABLED -> false;
        };
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static RedstoneMode byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IGNORED;
    }

    public Component displayName() {
        return Component.translatable("gui.resourcefulsheep.redstone." + name().toLowerCase(java.util.Locale.ROOT));
    }
}
