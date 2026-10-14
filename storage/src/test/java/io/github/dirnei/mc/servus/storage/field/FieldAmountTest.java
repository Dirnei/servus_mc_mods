package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class FieldAmountTest {

    @Test
    void emptyIsZero() {
        assertEquals(0, FieldAmount.parse(""));
        assertEquals(0, FieldAmount.parse(null));
    }

    @Test
    void digitsAreRead() {
        assertEquals(1000, FieldAmount.parse("1000"));
        assertEquals(7, FieldAmount.parse("007"));
    }

    @Test
    void anythingButDigitsIsZero() {
        assertEquals(0, FieldAmount.parse("-5"));
        assertEquals(0, FieldAmount.parse("12a"));
    }

    @Test
    void overflowSaturates() {
        assertEquals(Long.MAX_VALUE, FieldAmount.parse("99999999999999999999999"));
        assertEquals(Long.MAX_VALUE, FieldAmount.parse(String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    void levelsClampToTheLevelMaximum() {
        assertEquals(StockLevels.MAX, FieldAmount.HARVEST_LIMIT.clamp(250_000));
        assertEquals(StockLevels.MAX, FieldAmount.FERTILIZE_BELOW.clamp(Long.MAX_VALUE));
        assertEquals(0, FieldAmount.HARVEST_LIMIT.clamp(-1));
        assertEquals(640, FieldAmount.FERTILIZE_BELOW.clamp(640));
    }

    @Test
    void reserveClampsToTheReserveMaximum() {
        assertEquals(FertilizePlan.MAX_RESERVE, FieldAmount.RESERVE.clamp(99_999));
        assertEquals(0, FieldAmount.RESERVE.clamp(-10));
        assertEquals(FertilizePlan.MAX_RESERVE, FieldAmount.ANTI_CORROSION_RESERVE.clamp(99_999));
        assertEquals(0, FieldAmount.ANTI_CORROSION_RESERVE.clamp(-10));
    }

    @Test
    void levelsMapToStockLevels() {
        assertEquals(StockLevels.Level.HARVEST, FieldAmount.HARVEST_LIMIT.level());
        assertEquals(StockLevels.Level.FERTILIZE, FieldAmount.FERTILIZE_BELOW.level());
        assertNull(FieldAmount.RESERVE.level());
        assertNull(FieldAmount.ANTI_CORROSION_RESERVE.level());
    }

    @Test
    void unknownIdIsNull() {
        assertNull(FieldAmount.byId(4));
        assertNull(FieldAmount.byId(-1));
        assertEquals(FieldAmount.RESERVE, FieldAmount.byId(2));
        assertEquals(FieldAmount.ANTI_CORROSION_RESERVE, FieldAmount.byId(3));
    }
}
