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
}
