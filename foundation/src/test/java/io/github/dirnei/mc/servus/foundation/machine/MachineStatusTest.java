package io.github.dirnei.mc.servus.foundation.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MachineStatusTest {

    @Test
    void workingRuns() {
        assertEquals(MachineStatus.RUNNING, MachineStatus.of(true, false, true, MachineStatus.OUT_OF_FUEL));
    }

    @Test
    void waitingBeatsTheMissingFuel() {
        assertEquals(MachineStatus.WAITING, MachineStatus.of(false, true, false, MachineStatus.OUT_OF_FUEL));
    }

    @Test
    void workWithoutWorkingIsWhatIsMissing() {
        assertEquals(MachineStatus.OUT_OF_FUEL, MachineStatus.of(false, false, true, MachineStatus.OUT_OF_FUEL));
        assertEquals(MachineStatus.NO_POWER, MachineStatus.of(false, false, true, MachineStatus.NO_POWER));
    }

    @Test
    void noWorkIsIdle() {
        assertEquals(MachineStatus.IDLE, MachineStatus.of(false, false, false, MachineStatus.NO_POWER));
    }

    @Test
    void syncedOrdinalRoundTrips() {
        for (MachineStatus status : MachineStatus.values()) {
            assertEquals(status, MachineStatus.byOrdinal(status.ordinal()));
        }
        assertEquals(MachineStatus.IDLE, MachineStatus.byOrdinal(-1));
        assertEquals(MachineStatus.IDLE, MachineStatus.byOrdinal(99));
    }
}
