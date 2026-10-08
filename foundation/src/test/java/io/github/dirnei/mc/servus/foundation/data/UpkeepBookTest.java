package io.github.dirnei.mc.servus.foundation.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpkeepBookTest {

    @Test
    void firstCheckAsksTheMissingAmount() {
        UpkeepBook<String> book = new UpkeepBook<>();
        assertEquals(12, book.ask("smelter", 4, 16, 0));
    }

    @Test
    void levelMetAsksNothing() {
        UpkeepBook<String> book = new UpkeepBook<>();
        assertEquals(0, book.ask("smelter", 16, 16, 0));
        assertEquals(0, book.ask("smelter", 20, 16, 0));
    }

    @Test
    void openRequestBlocksAsking() {
        UpkeepBook<String> book = new UpkeepBook<>();
        book.asked("smelter", 12, 4, 0, 1200);
        assertEquals(0, book.ask("smelter", 10, 16, 40));
        assertTrue(book.isOpen("smelter"));
    }

    @Test
    void arrivalCompletesTheRequest() {
        UpkeepBook<String> book = new UpkeepBook<>();
        book.asked("smelter", 10, 2, 0, 1200);
        assertEquals(4, book.ask("smelter", 12, 16, 40));
        assertFalse(book.isOpen("smelter"));
    }

    @Test
    void arrivalsAddUpOverChecks() {
        UpkeepBook<String> book = new UpkeepBook<>();
        book.asked("smelter", 10, 2, 0, 1200);
        assertEquals(0, book.ask("smelter", 7, 16, 40));
        assertEquals(0, book.ask("smelter", 6, 16, 80));
        assertEquals(5, book.ask("smelter", 11, 16, 120));
    }

    @Test
    void timeoutReopens() {
        UpkeepBook<String> book = new UpkeepBook<>();
        book.asked("smelter", 12, 4, 0, 1200);
        assertEquals(0, book.ask("smelter", 4, 16, 1200));
        assertEquals(12, book.ask("smelter", 4, 16, 1201));
    }

    @Test
    void retainForgetsMembersThatLeft() {
        UpkeepBook<String> book = new UpkeepBook<>();
        book.asked("smelter", 12, 4, 0, 1200);
        book.asked("pump", 8, 8, 0, 1200);
        book.retain(List.of("pump"));
        assertFalse(book.isOpen("smelter"));
        assertTrue(book.isOpen("pump"));
    }
}
