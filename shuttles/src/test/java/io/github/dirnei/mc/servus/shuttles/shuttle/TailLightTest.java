package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.shuttle.StandReason.Kind;
import java.util.List;
import org.junit.jupiter.api.Test;

class TailLightTest {

    @Test
    void drivingIsDimAndBrakingBright() {
        assertEquals(TailLight.DIM_RED, TailLight.of(Kind.DRIVING, false, 0));
        assertEquals(TailLight.RED, TailLight.of(Kind.DRIVING, true, 0));
        assertEquals(TailLight.DIM_RED, TailLight.of(Kind.RING, false, 0));
        assertEquals(TailLight.RED, TailLight.of(Kind.RING, true, 0));
    }

    @Test
    void followingIsBright() {
        assertEquals(TailLight.RED, TailLight.of(Kind.FOLLOWING, false, 0));
        assertEquals(TailLight.RED, TailLight.of(Kind.FOLLOWING, true, 7));
    }

    @Test
    void waitsForOtherThingsBlinkAmberEveryTwentyTicks() {
        for (Kind kind : List.of(Kind.LIFT, Kind.DECK, Kind.POWER, Kind.CHUNK)) {
            assertEquals(TailLight.AMBER, TailLight.of(kind, false, 0), kind.name());
            assertEquals(TailLight.AMBER, TailLight.of(kind, false, 9), kind.name());
            assertEquals(TailLight.OFF, TailLight.of(kind, false, 10), kind.name());
            assertEquals(TailLight.OFF, TailLight.of(kind, true, 19), kind.name());
            assertEquals(TailLight.AMBER, TailLight.of(kind, false, 20), kind.name());
        }
    }

    @Test
    void workAndRidesAreOff() {
        for (Kind kind : List.of(Kind.STOP, Kind.FIELD_WORK, Kind.DOOR, Kind.HANDOVER, Kind.RIDING)) {
            assertEquals(TailLight.OFF, TailLight.of(kind, false, 0), kind.name());
            assertEquals(TailLight.OFF, TailLight.of(kind, true, 5), kind.name());
        }
    }
}
