package io.github.dirnei.mc.servus.foundation.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FilterWearTest {

    @Test
    void normalWearSpendsTheRequest() {
        FilterWear.Spend spend = new FilterWear(512, 0).spend(40);
        assertEquals(40, spend.spent());
        assertEquals(40, spend.used());
        assertFalse(spend.wornOut());
        assertEquals(472, new FilterWear(512, spend.used()).left());
    }

    @Test
    void wearingPastTheLastPointSpendsOnlyWhatIsLeft() {
        FilterWear.Spend spend = new FilterWear(512, 482).spend(40);
        assertEquals(30, spend.spent());
        assertEquals(512, spend.used());
        assertTrue(spend.wornOut());
    }

    @Test
    void wearingExactlyTheLastPointWearsItOut() {
        FilterWear.Spend spend = new FilterWear(512, 502).spend(10);
        assertEquals(10, spend.spent());
        assertTrue(spend.wornOut());
    }

    @Test
    void aWornOutFilterSpendsNothing() {
        FilterWear.Spend spend = new FilterWear(512, 512).spend(10);
        assertEquals(0, spend.spent());
        assertEquals(512, spend.used());
        assertTrue(spend.wornOut());
    }

    @Test
    void zeroAndNegativeRequestsSpendNothing() {
        FilterWear wear = new FilterWear(512, 100);
        assertEquals(0, wear.spend(0).spent());
        assertEquals(100, wear.spend(0).used());
        assertEquals(0, wear.spend(-5).spent());
        assertEquals(100, wear.spend(-5).used());
        assertFalse(wear.spend(-5).wornOut());
    }

    @Test
    void leftNeverDropsBelowZero() {
        assertEquals(0, new FilterWear(32, 40).left());
    }

    @Test
    void barIsFullWhenFresh() {
        assertEquals(13, new FilterWear(512, 0).barWidth(13));
    }

    @Test
    void barIsHalfWhenHalfIsUsed() {
        assertEquals(7, new FilterWear(512, 256).barWidth(13));
    }

    @Test
    void barShowsOnePointLeft() {
        assertEquals(1, new FilterWear(512, 511).barWidth(13));
    }

    @Test
    void barIsEmptyWhenWornOut() {
        assertEquals(0, new FilterWear(512, 512).barWidth(13));
    }

    @Test
    void barOfThePartlyUsedExample() {
        assertEquals(8, new FilterWear(512, 200).barWidth(13));
    }
}
