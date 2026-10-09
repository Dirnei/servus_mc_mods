package io.github.dirnei.mc.servus.foundation.side;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidSidesTest {

    @Test
    void defaultIsAll() {
        FluidSides sides = new FluidSides(3);
        for (RelativeSide side : RelativeSide.values()) {
            assertTrue(sides.isAll(side));
            assertTrue(sides.exposes(side, 0) && sides.exposes(side, 2));
        }
        assertTrue(sides.isDefault());
    }

    @Test
    void cycleRunsOffAllTanksOff() {
        FluidSides sides = new FluidSides(3);
        sides.set(RelativeSide.LEFT, FluidSides.OFF);
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            seen.add(sides.cycle(RelativeSide.LEFT));
        }
        assertEquals(List.of(FluidSides.ALL, 2, 3, 4, FluidSides.OFF), seen);
    }

    @Test
    void tankSideExposesOnlyItsTank() {
        FluidSides sides = new FluidSides(3);
        sides.set(RelativeSide.TOP, FluidSides.TANK_FIRST + 1);
        assertEquals(1, sides.tank(RelativeSide.TOP));
        assertTrue(sides.exposes(RelativeSide.TOP, 1));
        assertFalse(sides.exposes(RelativeSide.TOP, 0));
        assertFalse(sides.isDefault());
    }

    @Test
    void lockedSideStaysOff() {
        FluidSides sides = new FluidSides(3);
        sides.lock(RelativeSide.FRONT);
        assertEquals(FluidSides.OFF, sides.cycle(RelativeSide.FRONT));
        sides.set(RelativeSide.FRONT, FluidSides.ALL);
        assertTrue(sides.isOff(RelativeSide.FRONT));
        assertTrue(sides.isDefault());
    }

    @Test
    void encodingRoundTrip() {
        FluidSides sides = new FluidSides(3);
        sides.set(RelativeSide.LEFT, FluidSides.TANK_FIRST);
        sides.set(RelativeSide.RIGHT, FluidSides.OFF);
        FluidSides copy = new FluidSides(3);
        copy.fromBytes(sides.toBytes());
        for (RelativeSide side : RelativeSide.values()) {
            assertEquals(sides.value(side), copy.value(side));
        }
    }

    @Test
    void unknownTankDecodesToAll() {
        FluidSides many = new FluidSides(3);
        many.set(RelativeSide.LEFT, FluidSides.TANK_FIRST + 2);
        FluidSides few = new FluidSides(1);
        few.fromBytes(many.toBytes());
        assertTrue(few.isAll(RelativeSide.LEFT));
        few.fromBytes(new byte[0]);
        assertTrue(few.isDefault());
    }

    @Test
    void layerNextWraps() {
        assertEquals(SideLayer.FLUIDS, SideLayer.ITEMS.next());
        assertEquals(SideLayer.ITEMS, SideLayer.FLUIDS.next());
        assertEquals(SideLayer.ITEMS, SideLayer.byName("nonsense"));
    }
}
