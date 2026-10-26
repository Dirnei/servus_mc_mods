package io.github.dirnei.mc.servus.shuttles.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CapRoomTest {

    @Test
    void belowTheCap() {
        assertEquals(64, CapRoom.room(4096, 100, 64));
    }

    @Test
    void offerLargerThanTheRoom() {
        assertEquals(6, CapRoom.room(4096, 4090, 64));
    }

    @Test
    void atTheCap() {
        assertEquals(0, CapRoom.room(4096, 4096, 64));
    }

    @Test
    void aboveTheCap() {
        assertEquals(0, CapRoom.room(128, 200, 64));
    }

    @Test
    void noCapTakesAll() {
        assertEquals(64, CapRoom.room(0, 1_000_000, 64));
    }
}
