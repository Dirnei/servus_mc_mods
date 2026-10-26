package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static io.github.dirnei.mc.servus.foundation.logistics.duct.PipeOffer.NO_ALLOWANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PipeOfferTest {

    @Test
    void aFullChestOffersAStack() {
        assertEquals(64, PipeOffer.amount(200, 64, NO_ALLOWANCE));
    }

    @Test
    void keepAmountLeavesLess() {
        assertEquals(34, PipeOffer.amount(50 - 16, 64, NO_ALLOWANCE));
    }

    @Test
    void smallStackSize() {
        assertEquals(16, PipeOffer.amount(40, 16, NO_ALLOWANCE));
    }

    @Test
    void fillUpToAllowanceBounds() {
        assertEquals(10, PipeOffer.amount(200, 64, 10));
        assertEquals(0, PipeOffer.amount(200, 64, 0));
    }

    @Test
    void nothingMovableOffersNothing() {
        assertEquals(0, PipeOffer.amount(0, 64, NO_ALLOWANCE));
        assertEquals(0, PipeOffer.amount(-5, 64, NO_ALLOWANCE));
    }
}
