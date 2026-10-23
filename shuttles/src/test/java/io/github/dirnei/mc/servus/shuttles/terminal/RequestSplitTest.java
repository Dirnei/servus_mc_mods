package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RequestSplitTest {

    @Test
    void prefillIsAStackWhenCraftable() {
        assertEquals(64, RequestSplit.prefill(64, 10, true));
    }

    @Test
    void prefillIsNoMoreThanRequestable() {
        assertEquals(10, RequestSplit.prefill(64, 10, false));
        assertEquals(64, RequestSplit.prefill(64, 500, false));
        assertEquals(1, RequestSplit.prefill(64, 0, false));
        assertEquals(1, RequestSplit.prefill(1, 7, false));
    }

    @Test
    void capIsRequestableUnlessCraftable() {
        assertEquals(10, RequestSplit.cap(10, false));
        assertEquals(1, RequestSplit.cap(0, false));
        assertEquals(AmountSteps.MAX, RequestSplit.cap(10, true));
        assertEquals(AmountSteps.MAX, RequestSplit.cap(1_000_000, false));
    }

    @Test
    void allFromStock() {
        assertEquals(new RequestSplit(20, 0), RequestSplit.of(20, 50, true));
    }

    @Test
    void restCrafted() {
        RequestSplit split = RequestSplit.of(64, 10, true);
        assertEquals(new RequestSplit(10, 54), split);
        assertEquals(64, split.total());
    }

    @Test
    void cappedWhenTheRestCannotBeCrafted() {
        assertEquals(new RequestSplit(70, 0), RequestSplit.of(100, 70, false));
    }

    @Test
    void nothingAvailable() {
        assertEquals(new RequestSplit(0, 0), RequestSplit.of(5, 0, false));
        assertEquals(new RequestSplit(0, 8), RequestSplit.of(8, 0, true));
    }
}
