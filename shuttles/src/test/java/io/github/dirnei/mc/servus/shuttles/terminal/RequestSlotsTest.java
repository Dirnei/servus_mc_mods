package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.terminal.RequestSlots.Slot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestSlotsTest {

    private static final String COBBLE = "minecraft:cobblestone";
    private static final String BREAD = "minecraft:bread";

    @Test
    void sameItemFirst() {
        List<Slot<String>> slots = List.of(Slot.<String>empty(), Slot.<String>empty(), Slot.<String>empty(), new Slot<>(false, COBBLE, true));
        assertEquals(3, RequestSlots.choose(slots, COBBLE));
    }

    @Test
    void ownedSlotSkipped() {
        List<Slot<String>> slots = List.of(new Slot<>(true, null, true), Slot.<String>empty());
        assertEquals(1, RequestSlots.choose(slots, COBBLE));
    }

    @Test
    void ownedSlotOfSameItemSkipped() {
        List<Slot<String>> slots = List.of(new Slot<>(true, COBBLE, true), Slot.<String>empty());
        assertEquals(1, RequestSlots.choose(slots, COBBLE));
    }

    @Test
    void endedItemBlocksOtherItems() {
        List<Slot<String>> slots = List.of(new Slot<>(false, COBBLE, true), Slot.<String>empty());
        assertEquals(1, RequestSlots.choose(slots, BREAD));
    }

    @Test
    void fullRowGivesNone() {
        List<Slot<String>> slots = List.of(new Slot<>(true, null, true), new Slot<>(false, COBBLE, true));
        assertEquals(RequestSlots.NONE, RequestSlots.choose(slots, BREAD));
    }

    @Test
    void sameItemWithoutRoomSkipped() {
        List<Slot<String>> slots = List.of(new Slot<>(false, COBBLE, false), Slot.<String>empty());
        assertEquals(1, RequestSlots.choose(slots, COBBLE));
    }

    @Test
    void severalSlotsForMoreThanAStack() {
        List<Slot<String>> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            slots.add(Slot.empty());
        }
        assertEquals(List.of(0, 1, 2), RequestSlots.choose(slots, COBBLE, RequestSlots.needed(130, 64)));
    }

    @Test
    void asManyAsAreFree() {
        List<Slot<String>> slots = List.of(new Slot<>(true, null, true), Slot.<String>empty(), new Slot<>(false, BREAD, true), Slot.<String>empty());
        assertEquals(List.of(1, 3), RequestSlots.choose(slots, COBBLE, RequestSlots.needed(256, 64)));
    }

    @Test
    void sameItemFirstAmongSeveral() {
        List<Slot<String>> slots = List.of(Slot.<String>empty(), Slot.<String>empty(), new Slot<>(false, COBBLE, true));
        assertEquals(List.of(2, 0), RequestSlots.choose(slots, COBBLE, 2));
    }

    @Test
    void noneFreeGivesEmpty() {
        List<Slot<String>> slots = List.of(new Slot<>(true, null, true));
        assertEquals(List.of(), RequestSlots.choose(slots, COBBLE, 3));
    }

    @Test
    void neededRoundsUpAndIsAtLeastOne() {
        assertEquals(2, RequestSlots.needed(128, 64));
        assertEquals(3, RequestSlots.needed(130, 64));
        assertEquals(1, RequestSlots.needed(0, 64));
        assertEquals(1, RequestSlots.needed(16, 16));
    }
}
