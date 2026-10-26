package io.github.dirnei.mc.servus.foundation.logistics.duct;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KeepAmountsTest {

    @Test
    void keepLeavesTheAmount() {
        assertEquals(236, KeepAmounts.giveAllowance(300, 64, 1000));
        assertEquals(8, KeepAmounts.giveAllowance(300, 64, 8));
    }

    @Test
    void belowTheKeepAmountNothingLeaves() {
        assertEquals(0, KeepAmounts.giveAllowance(40, 64, 1000));
        assertEquals(0, KeepAmounts.giveAllowance(64, 64, 1000));
    }

    @Test
    void upToTakesTheMissingPart() {
        assertEquals(3, KeepAmounts.receiveAllowance(13, 16, 64));
        assertEquals(0, KeepAmounts.receiveAllowance(16, 16, 64));
        assertEquals(0, KeepAmounts.receiveAllowance(20, 16, 64));
        assertEquals(10, KeepAmounts.receiveAllowance(0, 16, 10));
    }

    @Test
    void noAmountPassesTheWholeOffer() {
        assertEquals(1000, KeepAmounts.giveAllowance(5, 0, 1000));
        assertEquals(64, KeepAmounts.receiveAllowance(5000, 0, 64));
        assertEquals(64, KeepAmounts.receiveAllowance(5000, -3, 64));
    }

    @Test
    void largeHeldAmountsDoNotOverflow() {
        long huge = Integer.MAX_VALUE * 3L;
        assertEquals(1000, KeepAmounts.giveAllowance(huge, 64, 1000));
        assertEquals(0, KeepAmounts.receiveAllowance(huge, Integer.MAX_VALUE, 1000));
        assertEquals(Integer.MAX_VALUE, KeepAmounts.giveAllowance(huge, 1, Integer.MAX_VALUE));
    }
}
