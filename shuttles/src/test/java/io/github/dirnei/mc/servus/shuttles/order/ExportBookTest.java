package io.github.dirnei.mc.servus.shuttles.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.order.ExportBook.Claim;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ExportBookTest {

    @Test
    void claimAnUnassignedExport() {
        ExportBook<String, String> book = new ExportBook<>();
        assertTrue(book.holder("wheat").isEmpty());
        assertTrue(book.claim("wheat", "outbound 3", 2));
        assertEquals(Optional.of(new Claim<>("outbound 3", 2)), book.holder("wheat"));
    }

    @Test
    void claimMovesFromAnotherStation() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("kelp", "smoker line", 0);
        assertTrue(book.claim("kelp", "outbound 3", 4));
        assertEquals(Optional.of(new Claim<>("outbound 3", 4)), book.holder("kelp"));
        assertTrue(book.slotsOf("smoker line").isEmpty());
    }

    @Test
    void occupiedSlotIsRefused() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("wheat", "outbound 3", 2);
        assertFalse(book.claim("kelp", "outbound 3", 2));
        assertTrue(book.holder("kelp").isEmpty());
        assertTrue(book.claim("kelp", "outbound 3", 3));
    }

    @Test
    void releaseBySlot() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("wheat", "outbound 3", 2);
        assertEquals(Optional.of("wheat"), book.releaseSlot("outbound 3", 2));
        assertTrue(book.holder("wheat").isEmpty());
        assertEquals(Optional.empty(), book.releaseSlot("outbound 3", 2));
    }

    @Test
    void releaseByStation() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("wheat", "outbound 3", 2);
        book.claim("kelp", "outbound 3", 5);
        book.claim("seeds", "outbound 1", 0);
        assertEquals(List.of("wheat", "kelp"), book.releaseStation("outbound 3"));
        assertEquals(Map.of(0, "seeds"), book.slotsOf("outbound 1"));
        assertTrue(book.slotsOf("outbound 3").isEmpty());
    }

    @Test
    void clearOnRuleRemoval() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("wheat", "outbound 3", 2);
        assertTrue(book.clear("wheat"));
        assertFalse(book.clear("wheat"));
        assertTrue(book.all().isEmpty());
    }

    @Test
    void slotsInSlotOrder() {
        ExportBook<String, String> book = new ExportBook<>();
        book.claim("kelp", "outbound 3", 5);
        book.claim("wheat", "outbound 3", 1);
        assertEquals(List.of(1, 5), List.copyOf(book.slotsOf("outbound 3").keySet()));
    }
}
