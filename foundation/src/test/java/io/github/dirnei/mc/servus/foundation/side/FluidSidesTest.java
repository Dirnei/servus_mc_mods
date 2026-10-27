package io.github.dirnei.mc.servus.foundation.side;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FluidSidesTest {

    private static FluidSides inbound(int tanks) {
        return new FluidSides(tanks, SideConfig.INBOUND_MODES, SideMode.INPUT);
    }

    @Test
    void defaultsAreFallbackOnAll() {
        FluidSides sides = inbound(3);
        for (RelativeSide side : RelativeSide.values()) {
            assertEquals(SideMode.INPUT, sides.mode(side));
            assertEquals(FluidSides.ALL, sides.tank(side));
            assertTrue(sides.reaches(side, 0) && sides.reaches(side, 2));
        }
        assertTrue(sides.isDefault());
    }

    @Test
    void modeCycleWrapsAndKeepsTank() {
        FluidSides sides = inbound(3);
        sides.setTank(RelativeSide.LEFT, 1);
        List<SideMode> seen = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            seen.add(sides.cycleMode(RelativeSide.LEFT));
        }
        assertEquals(List.of(SideMode.PULL, SideMode.DISABLED, SideMode.INPUT), seen);
        assertEquals(1, sides.tank(RelativeSide.LEFT));
    }

    @Test
    void tankCycleRunsAllThroughTanksAndKeepsMode() {
        FluidSides sides = inbound(3);
        sides.setMode(RelativeSide.BACK, SideMode.PULL);
        List<Integer> seen = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            seen.add(sides.cycleTank(RelativeSide.BACK));
        }
        assertEquals(List.of(0, 1, 2, FluidSides.ALL), seen);
        assertEquals(SideMode.PULL, sides.mode(RelativeSide.BACK));
    }

    @Test
    void tankSideReachesOnlyItsTank() {
        FluidSides sides = inbound(3);
        sides.setTank(RelativeSide.TOP, 1);
        assertTrue(sides.reaches(RelativeSide.TOP, 1));
        assertFalse(sides.reaches(RelativeSide.TOP, 0));
        assertFalse(sides.isDefault());
    }

    @Test
    void unsupportedModeFallsBack() {
        FluidSides sides = inbound(2);
        sides.setMode(RelativeSide.RIGHT, SideMode.PUSH);
        assertEquals(SideMode.INPUT, sides.mode(RelativeSide.RIGHT));
    }

    @Test
    void lockedSideStaysOffOnAll() {
        FluidSides sides = inbound(3);
        sides.lock(RelativeSide.FRONT);
        sides.setMode(RelativeSide.FRONT, SideMode.PULL);
        sides.setTank(RelativeSide.FRONT, 2);
        sides.cycleMode(RelativeSide.FRONT);
        sides.cycleTank(RelativeSide.FRONT);
        byte[] bytes = new byte[6];
        bytes[RelativeSide.FRONT.ordinal()] = (byte) FluidSides.pack(SideMode.PULL, 1);
        sides.fromBytes(bytes);
        assertTrue(sides.isLocked(RelativeSide.FRONT));
        assertEquals(SideMode.DISABLED, sides.mode(RelativeSide.FRONT));
        assertEquals(FluidSides.ALL, sides.tank(RelativeSide.FRONT));
    }

    @Test
    void encodingRoundTrips() {
        FluidSides sides = inbound(15);
        sides.setMode(RelativeSide.LEFT, SideMode.PULL);
        sides.setTank(RelativeSide.LEFT, 14);
        sides.setMode(RelativeSide.BACK, SideMode.DISABLED);
        sides.setTank(RelativeSide.TOP, 0);
        FluidSides copy = inbound(15);
        copy.fromBytes(sides.toBytes());
        for (RelativeSide side : RelativeSide.values()) {
            assertEquals(sides.mode(side), copy.mode(side));
            assertEquals(sides.tank(side), copy.tank(side));
        }
    }

    @Test
    void unknownTankDecodesToAll() {
        FluidSides sides = inbound(2);
        byte[] bytes = new byte[6];
        bytes[RelativeSide.LEFT.ordinal()] = (byte) FluidSides.pack(SideMode.PULL, 7);
        sides.fromBytes(bytes);
        assertEquals(SideMode.PULL, sides.mode(RelativeSide.LEFT));
        assertEquals(FluidSides.ALL, sides.tank(RelativeSide.LEFT));
    }

    @Test
    void missingBytesBecomeDefault() {
        FluidSides sides = inbound(2);
        sides.setMode(RelativeSide.LEFT, SideMode.PULL);
        sides.fromBytes(new byte[0]);
        assertTrue(sides.isDefault());
    }
}
