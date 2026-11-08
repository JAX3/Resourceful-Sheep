package com.jax3.resourcefulsheep.blockentity;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** What the Mechanical Separator is currently doing; shown in the GUI and synced via the menu. */
public enum MachineStatus {
    IDLE(0xFF9AA0A6),
    PROCESSING(0xFF55D66B),
    NO_ENERGY(0xFFE8B23A),
    OUTPUT_FULL(0xFFE35D5D),
    INVALID_INPUT(0xFFE35D5D),
    REDSTONE_DISABLED(0xFFB04040);

    /** ARGB colour of the status lamp and text. */
    public final int color;

    MachineStatus(int color) {
        this.color = color;
    }

    public static MachineStatus byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : IDLE;
    }

    public Component displayName() {
        return Component.translatable("gui.resourcefulsheep.status." + name().toLowerCase(Locale.ROOT));
    }

    public Component description() {
        return Component.translatable("gui.resourcefulsheep.status." + name().toLowerCase(Locale.ROOT) + ".desc");
    }
}
