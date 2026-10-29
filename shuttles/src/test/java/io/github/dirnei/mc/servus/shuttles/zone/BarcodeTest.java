package io.github.dirnei.mc.servus.shuttles.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BarcodeTest {

    @Test
    void sameCodeSameBars() {
        assertEquals(Barcode.bars("N8.W3+0", 30), Barcode.bars("N8.W3+0", 30));
    }

    @Test
    void otherCodeOtherBars() {
        assertNotEquals(Barcode.bars("N8.W3+0", 30), Barcode.bars("N8.W4+0", 30));
    }

    @Test
    void barsNeverTouchAndStayInside() {
        List<Barcode.Bar> bars = Barcode.bars("S12.E7-2", 30);
        for (int i = 0; i < bars.size(); i++) {
            Barcode.Bar bar = bars.get(i);
            assertTrue(bar.start() < bar.end() && bar.start() >= 0 && bar.end() <= 30);
            if (i > 0) {
                assertTrue(bars.get(i - 1).end() < bar.start(), "bars must leave a gap");
            }
        }
    }

    @Test
    void guardBarsAtBothEnds() {
        List<Barcode.Bar> bars = Barcode.bars("N0.E0+0", 30);
        assertEquals(new Barcode.Bar(0, 1), bars.get(0));
        assertEquals(new Barcode.Bar(29, 30), bars.get(bars.size() - 1));
    }
}
