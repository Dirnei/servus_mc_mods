package io.github.dirnei.mc.servus.alchemy.homunculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ElixirUpkeepTest {

    @Test
    void takesABottleOnlyWhenNoneIsInUse() {
        ElixirUpkeep upkeep = new ElixirUpkeep();
        AtomicInteger bottles = new AtomicInteger(2);
        for (int i = 0; i < 10; i++) {
            assertTrue(upkeep.feed(10, () -> bottles.getAndDecrement() > 0));
        }
        assertEquals(1, bottles.get(), "one bottle feeds ten ticks");
        assertEquals(0, upkeep.ticksLeft());
        assertTrue(upkeep.feed(10, () -> bottles.getAndDecrement() > 0));
        assertEquals(0, bottles.get(), "the eleventh tick opens the second bottle");
        assertEquals(9, upkeep.ticksLeft());
    }

    @Test
    void starvesWithoutABottle() {
        ElixirUpkeep upkeep = new ElixirUpkeep();
        assertFalse(upkeep.feed(10, () -> false));
        assertEquals(0, upkeep.ticksLeft());
    }

    @Test
    void savedBottleFeedsWithoutTakingAnother() {
        ElixirUpkeep upkeep = new ElixirUpkeep();
        upkeep.load(3);
        for (int i = 0; i < 3; i++) {
            assertTrue(upkeep.feed(10, () -> false));
        }
        assertFalse(upkeep.feed(10, () -> false));
    }
}
