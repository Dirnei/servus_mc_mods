package io.github.dirnei.mc.servus.shuttles.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineClassificationTest {

    private static final class Warehouse {
    }

    @Test
    void noMembersIsNone() {
        LineClassification.Result<Warehouse> result = LineClassification.classify(List.of());
        assertEquals(LineClassification.Kind.NONE, result.kind());
        assertNull(result.warehouse());
    }

    @Test
    void oneWarehouseSeenThroughSeveralMembers() {
        Warehouse base = new Warehouse();
        LineClassification.Result<Warehouse> result = LineClassification.classify(List.of(base, base, base));
        assertEquals(LineClassification.Kind.ONE, result.kind());
        assertSame(base, result.warehouse());
    }

    @Test
    void twoWarehousesConflict() {
        LineClassification.Result<Warehouse> result = LineClassification.classify(List.of(new Warehouse(), new Warehouse()));
        assertEquals(LineClassification.Kind.CONFLICT, result.kind());
        assertNull(result.warehouse());
    }

    @Test
    void threeWarehousesConflict() {
        Warehouse base = new Warehouse();
        LineClassification.Result<Warehouse> result = LineClassification.classify(List.of(base, new Warehouse(), base, new Warehouse()));
        assertEquals(LineClassification.Kind.CONFLICT, result.kind());
    }
}
