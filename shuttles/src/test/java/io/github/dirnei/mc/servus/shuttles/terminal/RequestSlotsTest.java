package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.shuttles.terminal.RequestSlots.Slot;
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
}
