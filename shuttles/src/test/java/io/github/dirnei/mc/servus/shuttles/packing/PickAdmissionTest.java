package io.github.dirnei.mc.servus.shuttles.packing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PickAdmissionTest {

    @Test
    void withinTheGrant() {
        assertTrue(PickAdmission.admits(64, 0, 0, 64));
    }

    @Test
    void beyondTheGrantWaits() {
        assertFalse(PickAdmission.admits(64, 0, 64, 64));
    }

    @Test
    void noEndpointFallsBackToWhatIsNeeded() {
        assertTrue(PickAdmission.admits(0, 32, 0, 32));
        assertFalse(PickAdmission.admits(0, 32, 32, 16));
    }

    @Test
    void zeroGrantAdmitsNothing() {
        assertFalse(PickAdmission.admits(0, 0, 0, 16));
    }

    @Test
    void smallRoomLetsOnePickThroughWhenNothingWaits() {
        assertTrue(PickAdmission.admits(30, 0, 0, 64));
        assertFalse(PickAdmission.admits(30, 0, 10, 64));
    }
}
