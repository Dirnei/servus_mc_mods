package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShaftsTest {

    private static NodePos n(int x, int y, int z) {
        return new NodePos(x, y, z);
    }

    @Test
    void oneColumnOfThree() {
        List<Shaft> shafts = Shafts.group(Set.of(n(0, 1, 0), n(0, 2, 0), n(0, 3, 0)));
        assertEquals(List.of(new Shaft(n(0, 1, 0), 3)), shafts);
        assertEquals(3, shafts.get(0).height());
        assertTrue(shafts.get(0).contains(n(0, 2, 0)));
        assertFalse(shafts.get(0).contains(n(0, 4, 0)));
    }

    @Test
    void gapGivesTwoShafts() {
        List<Shaft> shafts = Shafts.group(Set.of(n(0, 1, 0), n(0, 2, 0), n(0, 4, 0), n(0, 5, 0)));
        assertEquals(List.of(new Shaft(n(0, 1, 0), 2), new Shaft(n(0, 4, 0), 5)), shafts);
    }

    @Test
    void singleSegment() {
        assertEquals(List.of(new Shaft(n(3, 7, -2), 7)), Shafts.group(Set.of(n(3, 7, -2))));
    }

    @Test
    void columnsSideBySideStayApart() {
        List<Shaft> shafts = Shafts.group(Set.of(n(0, 0, 0), n(0, 1, 0), n(1, 0, 0), n(1, 1, 0)));
        assertEquals(List.of(new Shaft(n(0, 0, 0), 1), new Shaft(n(1, 0, 0), 1)), shafts);
    }

    @Test
    void floorsOfMapsEverySegment() {
        Shaft shaft = new Shaft(n(0, 1, 0), 3);
        Map<NodePos, Shaft> floors = Shafts.floorsOf(List.of(shaft));
        assertEquals(3, floors.size());
        assertEquals(shaft, floors.get(n(0, 3, 0)));
    }
}
