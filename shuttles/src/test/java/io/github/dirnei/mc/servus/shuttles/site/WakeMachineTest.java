package io.github.dirnei.mc.servus.shuttles.site;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.site.WakeAllowance.Reason;
import io.github.dirnei.mc.servus.shuttles.site.WakeMachine.Action;
import io.github.dirnei.mc.servus.shuttles.site.WakeMachine.State;
import org.junit.jupiter.api.Test;

class WakeMachineTest {

    private static final Reason OK = Reason.ALLOWED;

    @Test
    void unwantedStaysAsleep() {
        WakeMachine machine = new WakeMachine();
        assertEquals(Action.NONE, machine.tick(false, true, OK, 10));
        assertEquals(State.ASLEEP, machine.state());
    }

    @Test
    void wantedAndAllowedLoads() {
        WakeMachine machine = new WakeMachine();
        assertEquals(Action.LOAD, machine.tick(true, false, OK, 10));
        assertTrue(machine.awake());
    }

    @Test
    void refusedWaitsWithReasonThenLoads() {
        WakeMachine machine = new WakeMachine();
        assertEquals(Action.NONE, machine.tick(true, false, Reason.TEAM_LIMIT, 10));
        assertEquals(State.WAITING, machine.state());
        assertEquals(Reason.TEAM_LIMIT, machine.reason());
        assertEquals(Action.LOAD, machine.tick(true, false, OK, 10));
        assertEquals(Reason.ALLOWED, machine.reason());
    }

    @Test
    void waitingUnwantedFallsAsleep() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, Reason.FTB_CHUNKS, 10);
        assertEquals(Action.NONE, machine.tick(false, false, Reason.FTB_CHUNKS, 10));
        assertEquals(State.ASLEEP, machine.state());
    }

    @Test
    void busyKeepsItAwake() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 2);
        for (int i = 0; i < 50; i++) {
            assertEquals(Action.NONE, machine.tick(false, true, OK, 2));
        }
        assertEquals(State.AWAKE, machine.state());
    }

    @Test
    void graceCountsDownThenReleases() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 3);
        assertEquals(Action.NONE, machine.tick(false, false, OK, 3));
        assertEquals(State.GRACE, machine.state());
        assertEquals(Action.NONE, machine.tick(false, false, OK, 3));
        assertEquals(Action.NONE, machine.tick(false, false, OK, 3));
        assertEquals(Action.RELEASE, machine.tick(false, false, OK, 3));
        assertEquals(State.ASLEEP, machine.state());
        assertFalse(machine.awake());
    }

    @Test
    void newWorkInGraceReturnsToAwake() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 3);
        machine.tick(false, false, OK, 3);
        machine.tick(false, true, OK, 3);
        assertEquals(State.AWAKE, machine.state());
        machine.tick(false, false, OK, 3);
        assertEquals(2, machine.graceLeft(), "the grace starts over");
    }

    @Test
    void zeroGraceReleasesAtOnce() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 0);
        assertEquals(Action.RELEASE, machine.tick(false, false, OK, 0));
    }

    @Test
    void overCapacityWhileAwakeReleases() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 10);
        assertEquals(Action.RELEASE, machine.tick(true, true, Reason.OVER_CAPACITY, 10));
        assertEquals(State.WAITING, machine.state());
        assertEquals(Reason.OVER_CAPACITY, machine.reason());
        assertEquals(Action.NONE, machine.tick(true, false, Reason.OVER_CAPACITY, 10), "it never wakes while over capacity");
    }

    @Test
    void teamLimitIsNotCheckedWhileAwake() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 10);
        assertEquals(Action.NONE, machine.tick(true, false, Reason.TEAM_LIMIT, 10));
        assertEquals(State.AWAKE, machine.state());
    }

    @Test
    void stopReleasesAnAwakeSiteAndWaits() {
        WakeMachine machine = new WakeMachine();
        machine.tick(true, false, OK, 10);
        assertEquals(Action.RELEASE, machine.stop(Reason.NO_POWER));
        assertEquals(State.WAITING, machine.state());
        assertEquals(Reason.NO_POWER, machine.reason());
        // Still wanted but without the FE to start: it keeps waiting.
        assertEquals(Action.NONE, machine.tick(true, false, Reason.NO_POWER, 10));
        assertEquals(State.WAITING, machine.state());
        assertEquals(Action.LOAD, machine.tick(true, false, OK, 10));
    }

    @Test
    void stopLeavesASleepingSite() {
        WakeMachine machine = new WakeMachine();
        assertEquals(Action.NONE, machine.stop(Reason.NO_POWER));
        assertEquals(State.ASLEEP, machine.state());
    }
}
