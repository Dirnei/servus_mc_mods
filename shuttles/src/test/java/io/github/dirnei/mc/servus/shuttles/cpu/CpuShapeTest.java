package io.github.dirnei.mc.servus.shuttles.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CpuShapeTest {

    @Test
    void partIndexRoundTrip() {
        for (int part = 0; part < CpuShape.PARTS; part++) {
            CpuShape.Vec offset = CpuShape.offset(part);
            assertEquals(part, CpuShape.part(offset.x(), offset.y(), offset.z()));
        }
        assertEquals(0, CpuShape.part(0, 0, 0));
        assertEquals(26, CpuShape.part(2, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> CpuShape.part(3, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CpuShape.offset(27));
    }
}
