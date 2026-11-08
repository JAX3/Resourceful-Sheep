package com.jax3.resourcefulsheep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jax3.resourcefulsheep.blockentity.RedstoneMode;
import com.jax3.resourcefulsheep.blockentity.RelativeSide;
import com.jax3.resourcefulsheep.entity.SheepInfusion;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

/** Unit tests for logic that does not need a running game. In-world behaviour is covered by the game tests. */
class PureLogicTest {
    @Test
    void relativeSidesRoundTripForEveryFacing() {
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (RelativeSide side : RelativeSide.values()) {
                assertEquals(side, RelativeSide.of(facing, side.toDirection(facing)), facing + "/" + side);
            }
        }
        assertEquals(RelativeSide.FRONT, RelativeSide.of(Direction.NORTH, Direction.NORTH));
        assertEquals(RelativeSide.BACK, RelativeSide.of(Direction.NORTH, Direction.SOUTH));
        assertEquals(RelativeSide.TOP, RelativeSide.of(Direction.EAST, Direction.UP));
    }

    @Test
    void parsesSideNamesLeniently() {
        assertEquals(RelativeSide.LEFT, RelativeSide.parse(" left "));
        assertNull(RelativeSide.parse("sideways"));
    }

    @Test
    void redstoneModes() {
        assertTrue(RedstoneMode.IGNORED.allows(false));
        assertTrue(RedstoneMode.HIGH.allows(true));
        assertFalse(RedstoneMode.HIGH.allows(false));
        assertTrue(RedstoneMode.LOW.allows(false));
        assertFalse(RedstoneMode.DISABLED.allows(true));
        assertEquals(RedstoneMode.IGNORED, RedstoneMode.DISABLED.next());
        assertEquals(RedstoneMode.IGNORED, RedstoneMode.byId(99));
    }

    @Test
    void infusionCountsDownAndClears() {
        Identifier iron = Identifier.fromNamespaceAndPath("resourcefulsheep", "iron");
        SheepInfusion infusion = SheepInfusion.NONE.withResource(iron, 2);
        infusion = infusion.afterShearing(100, 0);
        assertEquals(Optional.of(iron), infusion.resource());
        assertEquals(1, infusion.usesRemaining());
        infusion = infusion.afterShearing(200, 50);
        assertFalse(infusion.isInfused());
        assertTrue(infusion.onCooldown(220));
        assertFalse(infusion.onCooldown(250));
    }

    @Test
    void permanentInfusionNeverRunsOut() {
        Identifier gold = Identifier.fromNamespaceAndPath("resourcefulsheep", "gold");
        SheepInfusion infusion = SheepInfusion.NONE.withResource(gold, -1);
        for (int i = 0; i < 100; i++) infusion = infusion.afterShearing(i, 0);
        assertTrue(infusion.isInfused());
    }
}
