package io.github.dirnei.mc.servus.foundation.energy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnergyDistributorTest {

    /** A buffer that can act as source and sink. */
    static final class Battery implements EnergySource, EnergySink {
        int stored;
        final int capacity;

        Battery(int stored, int capacity) {
            this.stored = stored;
            this.capacity = capacity;
        }

        @Override
        public int extract(int max, boolean simulate) {
            int taken = Math.min(max, stored);
            if (!simulate) {
                stored -= taken;
            }
            return taken;
        }

        @Override
        public int receive(int max, boolean simulate) {
            int accepted = Math.min(max, capacity - stored);
            if (!simulate) {
                stored += accepted;
            }
            return accepted;
        }
    }

    @Test
    void rateCapsTransfer() {
        Battery source = new Battery(1000, 1000);
        Battery sink = new Battery(0, 300);

        int moved = EnergyDistributor.distribute(List.of(source), List.of(sink), 100);

        assertEquals(100, moved);
        assertEquals(900, source.stored);
        assertEquals(100, sink.stored);
    }

    @Test
    void sinkLimitsTransfer() {
        Battery source = new Battery(1000, 1000);
        Battery sink = new Battery(270, 300);

        int moved = EnergyDistributor.distribute(List.of(source), List.of(sink), 100);

        assertEquals(30, moved);
        assertEquals(300, sink.stored);
    }

    @Test
    void shortSupplyIsSplitEqually() {
        Battery source = new Battery(100, 100);
        Battery a = new Battery(0, 100);
        Battery b = new Battery(0, 100);

        int moved = EnergyDistributor.distribute(List.of(source), List.of(a, b), 100);

        assertEquals(100, moved);
        assertEquals(50, a.stored);
        assertEquals(50, b.stored);
        assertEquals(0, source.stored);
    }

    @Test
    void leftoverFromSmallSinkGoesToOthers() {
        Battery source = new Battery(100, 100);
        Battery a = new Battery(80, 100);
        Battery b = new Battery(0, 100);

        EnergyDistributor.distribute(List.of(source), List.of(a, b), 100);

        assertEquals(100, a.stored);
        assertEquals(80, b.stored);
    }

    @Test
    void sourcesDrainInOrder() {
        Battery a = new Battery(100, 100);
        Battery b = new Battery(100, 100);
        Battery sink1 = new Battery(0, 100);
        Battery sink2 = new Battery(50, 100);

        int moved = EnergyDistributor.distribute(List.of(a, b), List.of(sink1, sink2), 100);

        assertEquals(150, moved);
        assertEquals(0, a.stored);
        assertEquals(50, b.stored);
        assertEquals(100, sink1.stored);
        assertEquals(100, sink2.stored);
    }

    @Test
    void nothingMovesWithoutSinksOrSources() {
        Battery source = new Battery(100, 100);
        Battery sink = new Battery(0, 100);

        assertEquals(0, EnergyDistributor.distribute(List.of(source), List.of(), 100));
        assertEquals(0, EnergyDistributor.distribute(List.of(), List.of(sink), 100));
        assertEquals(0, EnergyDistributor.distribute(List.of(source), List.of(sink), 0));
        assertEquals(100, source.stored);
    }

    @Test
    void allocationHandlesTinyBudgets() {
        assertArrayEquals(new int[] {1, 0, 0}, EnergyDistributor.allocate(new int[] {5, 5, 5}, 1));
        assertArrayEquals(new int[] {2, 2, 1}, EnergyDistributor.allocate(new int[] {5, 5, 5}, 5));
        assertArrayEquals(new int[] {0, 0, 0}, EnergyDistributor.allocate(new int[] {0, 0, 0}, 5));
    }
}
