package io.github.dirnei.mc.servus.foundation.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.data.UpkeepBook.Step;
import io.github.dirnei.mc.servus.foundation.data.UpkeepBook.TicketState;
import org.junit.jupiter.api.Test;

class UpkeepBookTest {

    private static UpkeepRequest asked(int amount, int count) {
        return new UpkeepRequest(1L, 7L, amount, count, 1200);
    }

    @Test
    void asksForMissingWhenBatchMissing() {
        Step step = UpkeepBook.step(null, 4, 16, 8, TicketState.OPEN, 0);
        assertEquals(12, step.ask());
        assertNull(step.keep());
        assertFalse(step.cancel());
    }

    @Test
    void asksNothingBelowBatch() {
        assertEquals(0, UpkeepBook.step(null, 14, 16, 8, TicketState.OPEN, 0).ask());
        assertEquals(0, UpkeepBook.step(null, 9, 16, 8, TicketState.OPEN, 0).ask());
        assertEquals(8, UpkeepBook.step(null, 8, 16, 8, TicketState.OPEN, 0).ask());
        assertEquals(0, UpkeepBook.step(null, 20, 16, 8, TicketState.OPEN, 0).ask());
    }

    @Test
    void asksWhenEmptyEvenBelowBatch() {
        assertEquals(1, UpkeepBook.step(null, 0, 1, 8, TicketState.OPEN, 0).ask());
    }

    @Test
    void batchCappedAtLevel() {
        assertEquals(0, UpkeepBook.step(null, 1, 4, 8, TicketState.OPEN, 0).ask());
        assertEquals(4, UpkeepBook.step(null, 0, 4, 8, TicketState.OPEN, 0).ask());
    }

    @Test
    void arrivalCompletesRequest() {
        Step step = UpkeepBook.step(asked(10, 2), 12, 16, 8, TicketState.OPEN, 40);
        assertNull(step.keep());
        assertFalse(step.cancel());
        assertEquals(0, step.ask());
    }

    @Test
    void partialArrivalKeepsRequest() {
        Step step = UpkeepBook.step(asked(10, 2), 7, 16, 8, TicketState.OPEN, 40);
        assertNotNull(step.keep());
        assertEquals(5, step.keep().remaining());
        assertEquals(7, step.keep().lastCount());
        assertEquals(0, step.ask());
        Step burned = UpkeepBook.step(step.keep(), 6, 16, 8, TicketState.OPEN, 80);
        assertEquals(5, burned.keep().remaining());
        assertEquals(6, burned.keep().lastCount());
    }

    @Test
    void closedTicketEndsWithoutCancel() {
        Step step = UpkeepBook.step(asked(10, 2), 6, 16, 8, TicketState.CLOSED, 40);
        assertNull(step.keep());
        assertFalse(step.cancel());
        assertEquals(10, step.ask());
    }

    @Test
    void timeoutWithOpenTicketCancels() {
        assertNotNull(UpkeepBook.step(asked(12, 4), 4, 16, 8, TicketState.OPEN, 1200).keep());
        Step step = UpkeepBook.step(asked(12, 4), 4, 16, 8, TicketState.OPEN, 1201);
        assertNull(step.keep());
        assertTrue(step.cancel());
        assertEquals(12, step.ask());
    }

    @Test
    void timeoutUnreachableKeepsRequest() {
        Step step = UpkeepBook.step(asked(12, 4), 4, 16, 8, TicketState.UNREACHABLE, 5000);
        assertNotNull(step.keep());
        assertFalse(step.cancel());
        assertEquals(0, step.ask());
    }

    @Test
    void askInSameStepAfterEnd() {
        Step step = UpkeepBook.step(asked(12, 4), 2, 16, 8, TicketState.CLOSED, 40);
        assertNull(step.keep());
        assertEquals(14, step.ask());
        Step belowBatch = UpkeepBook.step(asked(12, 4), 10, 16, 8, TicketState.CLOSED, 40);
        assertEquals(0, belowBatch.ask());
    }
}
