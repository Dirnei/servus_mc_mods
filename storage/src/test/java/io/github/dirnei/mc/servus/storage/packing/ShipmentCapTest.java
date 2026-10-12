package io.github.dirnei.mc.servus.storage.packing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ShipmentCapTest {

    @Test
    void capsInOrder() {
        assertEquals(List.of(0, 6), ShipmentCap.shortfalls(List.of(8, 8), 10));
    }

    @Test
    void nothingToCap() {
        assertEquals(List.of(0, 0), ShipmentCap.shortfalls(List.of(4, 4), 16));
    }

    @Test
    void firstShipmentShort() {
        assertEquals(List.of(5, 4), ShipmentCap.shortfalls(List.of(8, 4), 3));
    }

    @Test
    void relayKeepsWhatARunningTransferAsks() {
        assertEquals(0, ShipmentCap.relayCut(16, 16, 4, false));
    }

    @Test
    void relayFollowsAShortPick() {
        assertEquals(6, ShipmentCap.relayCut(16, 10, 4, false));
    }

    @Test
    void relayEndsWithWhatTheTransferDelivered() {
        assertEquals(4, ShipmentCap.relayCut(16, 16, 12, true));
    }

    @Test
    void relayAlreadyCut() {
        assertEquals(0, ShipmentCap.relayCut(8, 16, 12, true));
    }
}
