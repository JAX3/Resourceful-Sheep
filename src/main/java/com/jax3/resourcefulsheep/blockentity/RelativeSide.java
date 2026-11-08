package com.jax3.resourcefulsheep.blockentity;

import java.util.Locale;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/** A machine face expressed relative to the direction the machine's front is facing. */
public enum RelativeSide {
    FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM;

    public static @Nullable RelativeSide parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts an absolute world direction into a side relative to a horizontally-facing machine.
     *
     * @param facing the direction the front face points towards
     * @param side   the absolute side being queried
     */
    public static RelativeSide of(Direction facing, Direction side) {
        if (side == Direction.UP) return TOP;
        if (side == Direction.DOWN) return BOTTOM;
        if (side == facing) return FRONT;
        if (side == facing.getOpposite()) return BACK;
        // Looking at the machine's front, its "left" is the facing direction rotated clockwise.
        return side == facing.getClockWise() ? LEFT : RIGHT;
    }

    public Direction toDirection(Direction facing) {
        return switch (this) {
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getClockWise();
            case RIGHT -> facing.getCounterClockWise();
        };
    }
}
