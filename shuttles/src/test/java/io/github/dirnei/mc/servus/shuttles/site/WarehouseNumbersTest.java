package io.github.dirnei.mc.servus.shuttles.site;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WarehouseNumbersTest {

    @Test
    void firstWarehouseIsOne() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        assertEquals(1, numbers.numberOf(UUID.randomUUID(), 0));
        assertEquals(2, numbers.numberOf(UUID.randomUUID(), 0));
    }

    @Test
    void anIdKeepsItsNumber() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        numbers.numberOf(first, 0);
        numbers.numberOf(second, 0);
        assertEquals(2, numbers.numberOf(second, 0));
        assertEquals(1, numbers.numberOf(first, 5));
        assertEquals(3, numbers.numberOf(UUID.randomUUID(), 0));
    }

    @Test
    void numbersOfGoneWarehousesAreNotReused() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        UUID gone = UUID.randomUUID();
        numbers.numberOf(gone, 0);
        WarehouseNumbers loaded = new WarehouseNumbers();
        loaded.load(Map.of(), numbers.last());
        assertEquals(2, loaded.numberOf(UUID.randomUUID(), 0));
    }

    @Test
    void loadNeverFallsBelowAKnownNumber() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        numbers.load(Map.of(UUID.randomUUID(), 7), 3);
        assertEquals(8, numbers.next());
    }

    @Test
    void aHeldNumberBecomesTheIdsNumber() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        numbers.reserve(4);
        UUID id = UUID.randomUUID();
        assertEquals(2, numbers.numberOf(id, 2));
        assertEquals(2, numbers.known(id));
        assertEquals(5, numbers.numberOf(UUID.randomUUID(), 0));
    }

    @Test
    void oldRecordsAreNumberedByPosition() {
        WarehouseNumbers numbers = new WarehouseNumbers();
        numbers.reserve(1);
        List<WarehouseNumbers.Place> places = List.of(
                new WarehouseNumbers.Place("minecraft:the_nether", 0, 64, 0),
                new WarehouseNumbers.Place("minecraft:overworld", 100, 64, 0),
                new WarehouseNumbers.Place("minecraft:overworld", -20, 70, 5),
                new WarehouseNumbers.Place("minecraft:overworld", -20, 60, 5));
        assertArrayEquals(new int[] {5, 4, 3, 2}, numbers.numberInOrder(places));
    }
}
