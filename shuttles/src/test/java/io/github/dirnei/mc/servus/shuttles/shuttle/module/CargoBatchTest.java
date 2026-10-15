package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CargoBatchTest {

    private static final List<String> QUEUE = List.of("a1", "b1", "a2", "a3", "b2", "a4");

    @Test
    void keepsQueueOrderAndSkipsOtherWays() {
        assertEquals(List.of(0, 2, 3), CargoBatch.take(QUEUE, job -> job.startsWith("a"), 3));
        assertEquals(List.of(1, 4), CargoBatch.take(QUEUE, job -> job.startsWith("b"), 5));
    }

    @Test
    void stopsAtRoom() {
        assertEquals(List.of(0), CargoBatch.take(QUEUE, job -> job.startsWith("a"), 1));
    }

    @Test
    void zeroRoomTakesNothing() {
        assertEquals(List.of(), CargoBatch.take(QUEUE, job -> true, 0));
    }
}
