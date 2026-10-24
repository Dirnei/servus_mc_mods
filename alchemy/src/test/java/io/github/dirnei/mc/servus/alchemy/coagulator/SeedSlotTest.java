package io.github.dirnei.mc.servus.alchemy.coagulator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SeedSlotTest {

    @Test
    void anEmptySlotTakesAStack() {
        assertEquals(64, SeedSlot.room(0, false, 64));
        assertEquals(16, SeedSlot.room(0, false, 16));
    }

    @Test
    void moreOfTheSameSeedFillsTheStack() {
        assertEquals(61, SeedSlot.room(3, true, 64));
        assertEquals(13, SeedSlot.room(3, true, 16));
    }

    @Test
    void aFullStackTakesNoMore() {
        assertEquals(0, SeedSlot.room(64, true, 64));
    }

    @Test
    void anotherKindIsRefused() {
        assertEquals(0, SeedSlot.room(3, false, 64));
        assertEquals(0, SeedSlot.room(1, false, 64));
    }
}
