package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequestRoomTest {

    @Test
    void roomOfTwoEmptySlots() {
        assertEquals(128, RequestRoom.grant(128, 128, List.of(0, 0), 64, 0));
    }

    @Test
    void fullSlotGrantsNothing() {
        assertEquals(0, RequestRoom.grant(64, 64, List.of(64), 64, 0));
    }

    @Test
    void partlyTakenSlot() {
        assertEquals(30, RequestRoom.grant(64, 64, List.of(34), 64, 0));
    }

    @Test
    void heldGrantsReserveRoom() {
        assertEquals(64, RequestRoom.grant(128, 128, List.of(0, 0), 64, 64));
    }

    @Test
    void neverMoreThanTheRequestNeeds() {
        assertEquals(10, RequestRoom.grant(64, 10, List.of(0), 64, 0));
    }

    @Test
    void slotOfAnotherItemHasNoRoom() {
        assertEquals(64, RequestRoom.grant(128, 128, List.of(RequestRoom.OTHER_ITEM, 0), 64, 0));
    }

    @Test
    void neverBelowZero() {
        assertEquals(0, RequestRoom.grant(16, 16, List.of(60), 64, 10));
    }
}
