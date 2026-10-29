package io.github.dirnei.mc.servus.shuttles.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.client.ShuttleSoundPick.Mover;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShuttleSoundPickTest {

    private static Mover<String> at(String key, double x, double speed) {
        return new Mover<>(key, x, 0, 0, speed);
    }

    @Test
    void picksTheNearestFourOfTen() {
        List<Mover<String>> movers = new ArrayList<>();
        int[] distances = {9, 3, 7, 1, 10, 5, 2, 8, 6, 4};
        for (int d : distances) {
            movers.add(at("s" + d, d, 0.1));
        }
        assertEquals(List.of("s1", "s2", "s3", "s4"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 4));
    }

    @Test
    void picksAllWhenFewerThanTheLimit() {
        List<Mover<String>> movers = List.of(at("a", 5, 0.1), at("b", 2, 0.1));
        assertEquals(List.of("b", "a"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 4));
    }

    @Test
    void tiesKeepTheirOrder() {
        List<Mover<String>> movers = List.of(at("a", 3, 0.1), at("b", -3, 0.1), at("c", 3, 0.1), at("d", 1, 0.1));
        assertEquals(List.of("d", "a", "b"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 3));
        assertEquals(List.of("d", "a", "b"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 3), "same pick again");
    }

    @Test
    void standingShuttlesAreNeverPicked() {
        List<Mover<String>> movers = List.of(at("standing", 1, 0), at("far", 9, 0.05));
        assertEquals(List.of("far"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 4));
    }

    @Test
    void measuresInThreeDimensions() {
        List<Mover<String>> movers = List.of(new Mover<>("above", 0, 6, 0, 0.1), new Mover<>("beside", 0, 0, 4, 0.1));
        assertEquals(List.of("beside"), ShuttleSoundPick.nearest(movers, 0, 0, 0, 1));
    }

    @Test
    void fasterGivesHigherPitch() {
        float slow = ShuttleSoundPick.pitch(0.12, 0.15, 1);
        float fast = ShuttleSoundPick.pitch(0.25, 0.15, 1);
        assertTrue(fast > slow, fast + " > " + slow);
        assertEquals(1.0F, ShuttleSoundPick.pitch(0.15, 0.15, 1), 1e-6);
    }

    @Test
    void pitchClampsAtBothEnds() {
        assertEquals(ShuttleSoundPick.MIN_PITCH, ShuttleSoundPick.pitch(0.001, 0.15, 1), 1e-6);
        assertEquals(ShuttleSoundPick.MAX_PITCH, ShuttleSoundPick.pitch(5, 0.15, 1), 1e-6);
        assertEquals(ShuttleSoundPick.MAX_PITCH * 1.05F, ShuttleSoundPick.pitch(5, 0.15, 1.05F), 1e-6);
    }

    @Test
    void jitterIsStablePerKeyAndWithinRange() {
        assertEquals(ShuttleSoundPick.jitter(42), ShuttleSoundPick.jitter(42));
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        for (long key = 0; key < 1000; key++) {
            float jitter = ShuttleSoundPick.jitter(key);
            assertTrue(jitter >= ShuttleSoundPick.MIN_JITTER && jitter <= ShuttleSoundPick.MAX_JITTER, "key " + key + ": " + jitter);
            low = Math.min(low, jitter);
            high = Math.max(high, jitter);
        }
        assertTrue(high - low > 0.08F, "neighbouring keys spread over the range");
    }
}
