package io.github.dirnei.mc.servus.storage.cpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CpuChoiceTest {

    @Test
    void firstFreeSlotWins() {
        List<CpuChoice.Load> loads = List.of(new CpuChoice.Load(2, 2), new CpuChoice.Load(1, 2), new CpuChoice.Load(0, 2));
        assertEquals(1, CpuChoice.choose(loads).getAsInt());
    }

    @Test
    void allFullPicksFewestOpen() {
        List<CpuChoice.Load> loads = List.of(new CpuChoice.Load(4, 2), new CpuChoice.Load(3, 2), new CpuChoice.Load(5, 2));
        assertEquals(1, CpuChoice.choose(loads).getAsInt());
    }

    @Test
    void tieGoesToEarliest() {
        List<CpuChoice.Load> loads = List.of(new CpuChoice.Load(3, 2), new CpuChoice.Load(3, 2));
        assertEquals(0, CpuChoice.choose(loads).getAsInt());
        List<CpuChoice.Load> free = List.of(new CpuChoice.Load(0, 2), new CpuChoice.Load(0, 2));
        assertEquals(0, CpuChoice.choose(free).getAsInt());
    }

    @Test
    void emptyListChoosesNothing() {
        assertTrue(CpuChoice.choose(List.of()).isEmpty());
    }
}
