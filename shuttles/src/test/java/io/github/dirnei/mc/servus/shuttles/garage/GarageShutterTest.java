package io.github.dirnei.mc.servus.shuttles.garage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GarageShutterTest {

    @Test
    void openWhileALeavingShuttleWaitsInside() {
        assertTrue(GarageShutter.wantsOpen(true, false, 8, 0, 0));
    }

    @Test
    void staysOpenUntilTheLeavingShuttleIsClear() {
        assertTrue(GarageShutter.wantsOpen(true, false, 8, 1.4, 0.15));
        assertFalse(GarageShutter.wantsOpen(true, false, 8, 1.6, 0.15));
    }

    @Test
    void opensAheadOfAShuttleComingHome() {
        double ahead = GarageShutter.CLEAR + 0.15 * GarageShutter.TRAVEL_TICKS;
        assertTrue(GarageShutter.wantsOpen(false, true, 10, 9 - ahead + 0.01, 0.15));
        assertFalse(GarageShutter.wantsOpen(false, true, 10, 9 - ahead - 0.1, 0.15));
    }

    @Test
    void shutsOnceTheShuttleStandsInside() {
        assertTrue(GarageShutter.wantsOpen(false, true, 10, 8.9, 0.15));
        assertFalse(GarageShutter.wantsOpen(false, true, 10, 9, 0.15));
    }

    @Test
    void aShuttleOutOnTheRailsWantsNothing() {
        assertFalse(GarageShutter.wantsOpen(false, false, 10, 0, 0.15));
    }

    @Test
    void pullingInIsNotFree() {
        double ahead = GarageShutter.CLEAR + 0.15 * GarageShutter.TRAVEL_TICKS;
        assertTrue(GarageShutter.pullingIn(10, 9 - ahead + 0.01, 0.15));
        assertTrue(GarageShutter.pullingIn(10, 8.9, 0.15));
        assertFalse(GarageShutter.pullingIn(10, 9, 0.15), "a parked shuttle is free again");
    }

    @Test
    void farFromHomeIsFree() {
        double ahead = GarageShutter.CLEAR + 0.15 * GarageShutter.TRAVEL_TICKS;
        assertFalse(GarageShutter.pullingIn(10, 9 - ahead - 0.1, 0.15));
        assertFalse(GarageShutter.pullingIn(10, 0, 0.15));
    }

    @Test
    void movesAtItsTravelSpeedAndStopsAtTheEnds() {
        assertEquals(0.5, GarageShutter.step(0, true, GarageShutter.TRAVEL_TICKS / 2), 1e-9);
        assertEquals(1, GarageShutter.step(0.9, true, GarageShutter.TRAVEL_TICKS), 1e-9);
        assertEquals(0, GarageShutter.step(0.1, false, GarageShutter.TRAVEL_TICKS), 1e-9);
        assertEquals(0.4, GarageShutter.step(0.4, true, -3), 1e-9);
    }

    @Test
    void slatsStackUpIntoTheLintel() {
        float[] shut = GarageDoorLook.slatBottoms(0);
        assertEquals(GarageDoorLook.SLAT_INSET, shut[0], 1e-6);
        float topSlatTop = shut[GarageDoorLook.SLATS - 1] + GarageDoorLook.SLAT_HEIGHT - GarageDoorLook.SLAT_GAP;
        assertTrue(topSlatTop < GarageDoorLook.DOOR_TOP, "the shut curtain ends below the lintel, at " + topSlatTop);
        for (float bottom : GarageDoorLook.slatBottoms(1)) {
            assertTrue(Float.isNaN(bottom), "an open shutter shows no slat");
        }
        float[] half = GarageDoorLook.slatBottoms(0.5);
        assertEquals(6 + GarageDoorLook.SLAT_INSET, half[0], 1e-6);
        assertTrue(Float.isNaN(half[GarageDoorLook.SLATS - 1]), "the top slats are rolled up");
    }
}
