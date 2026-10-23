package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ShuttleCounterTest {

    @Test
    void startsAtOne() {
        ShuttleCounter counter = new ShuttleCounter(0);
        assertEquals(0, counter.last());
        assertEquals(1, counter.take());
    }

    @Test
    void countsUp() {
        ShuttleCounter counter = new ShuttleCounter(0);
        counter.take();
        assertEquals(2, counter.take());
        assertEquals(3, counter.take());
        assertEquals(3, counter.last());
    }

    @Test
    void continuesFromLoadedLast() {
        assertEquals(13, new ShuttleCounter(12).take());
    }

    @Test
    void negativeLastStartsFresh() {
        assertEquals(1, new ShuttleCounter(-4).take());
    }
}
