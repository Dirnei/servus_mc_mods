package io.github.dirnei.mc.servus.shuttles.packing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class SlotGrantTest {

    @Test
    void twoFreeSlotsGrantTwoStacks() {
        List<SlotGrant.Part> parts = SlotGrant.plan(List.of(3, 5), 64, 200);
        assertEquals(List.of(new SlotGrant.Part(3, 64), new SlotGrant.Part(5, 64)), parts);
        assertEquals(128, SlotGrant.total(parts));
    }

    @Test
    void enderPearlsTakeOneSlot() {
        assertEquals(List.of(new SlotGrant.Part(0, 16)), SlotGrant.plan(List.of(0, 1, 2), 16, 16));
    }

    @Test
    void exactFit() {
        List<SlotGrant.Part> parts = SlotGrant.plan(List.of(0, 1), 64, 128);
        assertEquals(2, parts.size());
        assertEquals(128, SlotGrant.total(parts));
    }

    @Test
    void noFreeSlotGrantsNothing() {
        assertEquals(List.of(), SlotGrant.plan(List.of(), 64, 64));
    }

    @Test
    void lessThanAStackTakesOneSlot() {
        assertEquals(List.of(new SlotGrant.Part(4, 3)), SlotGrant.plan(List.of(4, 6), 64, 3));
    }

    @Test
    void lastSlotHoldsTheRest() {
        assertEquals(List.of(new SlotGrant.Part(0, 64), new SlotGrant.Part(1, 6)), SlotGrant.plan(List.of(0, 1, 2), 64, 70));
    }
}
