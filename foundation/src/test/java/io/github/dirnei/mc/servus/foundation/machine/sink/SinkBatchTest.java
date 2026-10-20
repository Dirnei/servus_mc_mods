package io.github.dirnei.mc.servus.foundation.machine.sink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SinkBatchTest {

    @Test
    void budgetWalksFromTheFirstIndex() {
        assertArrayEquals(new int[]{3, 5, 0}, SinkBatch.plan(new int[]{3, 64, 10}, 8));
    }

    @Test
    void unlimitedBudgetTakesEverything() {
        assertArrayEquals(new int[]{64, 64, 7}, SinkBatch.plan(new int[]{64, 64, 7}, SinkRates.UNLIMITED));
    }

    @Test
    void zeroBudgetTakesNothing() {
        assertArrayEquals(new int[]{0, 0}, SinkBatch.plan(new int[]{5, 5}, 0));
    }

    @Test
    void emptyIndicesAreSkipped() {
        assertArrayEquals(new int[]{0, 2, 0, 2}, SinkBatch.plan(new int[]{0, 2, 0, 9}, 4));
    }

    @Test
    void budgetLargerThanTheContentsTakesAll() {
        assertArrayEquals(new int[]{1, 2, 3}, SinkBatch.plan(new int[]{1, 2, 3}, 1000));
    }

    @Test
    void fractionalBudgetRoundsDown() {
        assertArrayEquals(new int[]{2, 0}, SinkBatch.plan(new int[]{2, 5}, 2.9));
    }
}
