package io.github.dirnei.mc.servus.storage.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class CpuShapeTest {

    @Test
    void twentySevenOriginsLowestFirst() {
        List<CpuShape.Vec> origins = CpuShape.origins(10, 5, 20);
        assertEquals(27, origins.size());
        assertEquals(27, new HashSet<>(origins).size());
        assertEquals(new CpuShape.Vec(8, 3, 18), origins.get(0));
        assertEquals(new CpuShape.Vec(10, 5, 20), origins.get(26));
        for (CpuShape.Vec origin : origins) {
            int dx = 10 - origin.x();
            int dy = 5 - origin.y();
            int dz = 20 - origin.z();
            CpuShape.part(dx, dy, dz);
        }
    }

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
