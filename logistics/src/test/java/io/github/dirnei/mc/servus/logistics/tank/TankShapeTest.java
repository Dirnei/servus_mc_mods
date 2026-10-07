package io.github.dirnei.mc.servus.logistics.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TankShapeTest {

    private static List<NodePos> box(int ox, int oy, int oz, int w, int h, int d) {
        List<NodePos> list = new ArrayList<>();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < d; z++) {
                    list.add(new NodePos(ox + x, oy + y, oz + z));
                }
            }
        }
        return list;
    }

    @Test
    void fullBoxIsFormed() {
        TankShape shape = TankShape.of(box(3, 64, -2, 2, 3, 2));
        assertTrue(shape.isBox());
        assertEquals(4, shape.blocksPerLayer());
        assertEquals(3, shape.height());
        assertEquals(12, shape.volume());
    }

    @Test
    void lShapeIsNotFormed() {
        TankShape shape = TankShape.of(List.of(new NodePos(0, 0, 0), new NodePos(1, 0, 0), new NodePos(0, 0, 1)));
        assertFalse(shape.isBox());
    }

    @Test
    void boxPlusTouchingBlockIsNotFormed() {
        List<NodePos> blocks = box(0, 0, 0, 2, 2, 2);
        blocks.add(new NodePos(2, 0, 0));
        assertFalse(TankShape.of(blocks).isBox());
    }

    @Test
    void singleBlockIsABox() {
        TankShape shape = TankShape.of(List.of(new NodePos(5, 5, 5)));
        assertTrue(shape.isBox());
        assertEquals(1, shape.volume());
    }

    @Test
    void layerAndIndexFollowFixedOrder() {
        TankShape shape = TankShape.of(box(10, 20, 30, 2, 2, 2));
        assertEquals(0, shape.layer(new NodePos(10, 20, 30)));
        assertEquals(1, shape.layer(new NodePos(11, 21, 31)));
        assertEquals(0, shape.indexInLayer(new NodePos(10, 21, 30)));
        assertEquals(1, shape.indexInLayer(new NodePos(11, 20, 30)));
        assertEquals(2, shape.indexInLayer(new NodePos(10, 20, 31)));
        assertEquals(3, shape.indexInLayer(new NodePos(11, 20, 31)));
    }
}
