package io.github.dirnei.mc.servus.foundation.logistics.tank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TankLayersTest {

    private static final int CAP = 16_000;

    @Test
    void towerHalfFull() {
        TankLayers layers = TankLayers.of(1, 4, CAP, 2 * CAP);
        assertEquals(CAP, layers.amount(0, 0));
        assertEquals(CAP, layers.amount(1, 0));
        assertEquals(0, layers.amount(2, 0));
        assertEquals(0, layers.amount(3, 0));
    }

    @Test
    void twoByTwoByTwoWithSixBlocksWorth() {
        TankLayers layers = TankLayers.of(4, 2, CAP, 6L * CAP);
        for (int i = 0; i < 4; i++) {
            assertEquals(CAP, layers.amount(0, i));
            assertEquals(CAP / 2, layers.amount(1, i));
        }
    }

    @Test
    void emptyTank() {
        TankLayers layers = TankLayers.of(4, 3, CAP, 0);
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 4; i++) {
                assertEquals(0, layers.amount(layer, i));
            }
        }
    }

    @Test
    void totalIsClampedToCapacity() {
        TankLayers layers = TankLayers.of(2, 2, CAP, 10L * CAP);
        assertEquals(4L * CAP, layers.total());
        assertEquals(CAP, layers.amount(1, 1));
        assertEquals(0, TankLayers.of(2, 2, CAP, -5).total());
    }

    @Test
    void remainderSumsExactly() {
        long total = 3L * CAP + 7;
        TankLayers layers = TankLayers.of(3, 2, CAP, total);
        long sum = 0;
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < 3; i++) {
                sum += layers.amount(layer, i);
            }
        }
        assertEquals(total, sum);
        assertEquals(3, layers.amount(1, 0));
        assertEquals(2, layers.amount(1, 2));
    }
}
