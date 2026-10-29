package io.github.dirnei.mc.servus.shuttles.fluidstation;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.StationArmLook.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StationArmLookTest {

    private static final float STOP = 40;

    @Test
    void swingsDownLowersHoldsAndGoesBack() {
        assertEquals(0, swing(STOP, 0), 1e-6);
        assertEquals(0.5F, swing(STOP, SWING_TICKS / 2F), 1e-6);
        assertEquals(1, swing(STOP, SWING_TICKS), 1e-6);
        assertEquals(0, lower(STOP, SWING_TICKS), 1e-6);
        assertEquals(1, lower(STOP, SWING_TICKS + LOWER_TICKS), 1e-6);
        assertEquals(1, lower(STOP, STOP / 2), 1e-6);
        assertEquals(0, lower(STOP, STOP - SWING_TICKS), 1e-6);
        assertEquals(1, swing(STOP, STOP - SWING_TICKS), 1e-6);
        assertEquals(0, swing(STOP, STOP), 1e-6);
    }

    @Test
    void hoseMovesOnlyWhileTheBoomLiesLevel() {
        for (float elapsed = 0; elapsed <= STOP; elapsed += 0.5F) {
            assertTrue(lower(STOP, elapsed) == 0 || swing(STOP, elapsed) == 1, "at " + elapsed);
        }
    }

    @Test
    void shortStopNeverLowersTheHose() {
        float stop = 2 * SWING_TICKS;
        for (float elapsed = 0; elapsed <= stop; elapsed += 0.5F) {
            assertEquals(0, lower(stop, elapsed), 1e-6);
        }
        assertEquals(0, swing(0, 0), 1e-6);
    }

    @Test
    void earlyEndGoesBackFromWhereTheArmIs() {
        for (int at = 0; at <= 40; at++) {
            int ticks = endedEarly(40, at);
            assertEquals(swing(STOP, at), swing(ticks, at), 1e-6, "swing at " + at);
            assertEquals(lower(STOP, at), lower(ticks, at), 1e-6, "lower at " + at);
            assertEquals(0, swing(ticks, ticks), 1e-6);
        }
        assertEquals(40, endedEarly(40, 50));
    }

    @Test
    void angleRunsFromUprightToLevel() {
        assertEquals(90, angle(0), 1e-6);
        assertEquals(0, angle(1), 1e-6);
    }

    @Test
    void dockedHeadSitsAroundTheFillerCap() {
        float headBottom = PIVOT_Y + headTop(hoseDrop()) - HEAD_HEIGHT;
        assertEquals(capTop() - HEAD_SINK, headBottom, 1e-4);
        assertTrue(hoseDrop() > 1, "the hose drop shows");
        assertEquals(8, PIVOT_OUT + HOSE_AT, 1e-6);
        assertTrue(BOOM_LENGTH > HOSE_AT + HEAD_HALF - 1 && BOOM_LENGTH < HOSE_AT + HEAD_HALF, "the boom ends inside the head's reach");
    }

    @Test
    void armStaysInFrontOfTheStationAndClearOfTheStub() {
        assertTrue(PIVOT_OUT - SWIVEL_HALF > 0, "the swivel stands off the front face");
        float lowest = Float.MAX_VALUE;
        float nearest = Float.MAX_VALUE;
        for (int degrees = 0; degrees <= 90; degrees++) {
            double a = Math.toRadians(degrees);
            for (float side : new float[] {-BOOM_HALF, BOOM_HALF}) {
                float y = (float) (PIVOT_Y - BOOM_BACK * Math.sin(a) + side * Math.cos(a));
                float out = (float) (PIVOT_OUT - BOOM_BACK * Math.cos(a) - side * Math.sin(a));
                lowest = Math.min(lowest, y);
                nearest = Math.min(nearest, out);
            }
        }
        assertTrue(lowest > STUB_Y1, "the boom's back end clears the stub, lowest " + lowest);
        assertTrue(nearest > 0, "the boom never reaches into the station, nearest " + nearest);
        assertTrue(STUB_Y1 > PIVOT_Y - SWIVEL_HALF && STUB_Y1 < PIVOT_Y, "the stub ends inside the swivel");
    }

    @Test
    void raisedArmClearsTheTanker() {
        float headBottom = PIVOT_Y + headTop(0) - HEAD_HEIGHT;
        assertTrue(headBottom > capTop(), "the raised head swings over the cap");
    }

    @Test
    void noSharedPlanesBetweenParts() {
        assertTrue(STUB_HALF < BOOM_HALF && BOOM_HALF < SWIVEL_HALF_X, "stub, boom and swivel differ in x");
        assertTrue(HOSE_HALF < BOOM_HALF && BOOM_HALF < HEAD_HALF, "hose, boom and head differ in x");
        assertTrue(HOSE_TOP > -BOOM_HALF, "the hose starts inside the boom");
        assertTrue(HEAD_GAP > 0 && HOSE_INTO_HEAD > HEAD_GAP && HOSE_INTO_HEAD < HEAD_HEIGHT, "the hose ends inside the head");
        assertTrue(HOSE_AT + HEAD_HALF != BOOM_LENGTH && HOSE_AT + HOSE_HALF != BOOM_LENGTH);
        assertTrue(BOOM_BACK < SWIVEL_HALF, "the boom's back end stays inside the swivel");
    }
}
