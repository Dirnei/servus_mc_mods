package io.github.dirnei.mc.servus.storage.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HandoffTest {

    @Test
    void allToDeliveringDependant() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, true, false)), 0, 0, 0, 100);
        assertEquals(List.of(4), split.handed());
        assertEquals(0, split.hold());
        assertEquals(0, split.toStorage());
    }

    @Test
    void surplusGoesToStorage() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(2, true, false)), 0, 0, 0, 100);
        assertEquals(List.of(2), split.handed());
        assertEquals(0, split.hold());
        assertEquals(2, split.toStorage());
    }

    @Test
    void holdsWhileDependantWaits() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(3, false, false)), 0, 0, 40, 100);
        assertEquals(List.of(0), split.handed());
        assertEquals(3, split.hold());
        assertEquals(1, split.toStorage());
    }

    @Test
    void holdExpires() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, false)), 0, 0, 100, 100);
        assertEquals(0, split.hold());
        assertEquals(4, split.toStorage());
    }

    @Test
    void sameProducerGoesToStorage() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, true)), 0, 0, 0, 100);
        assertEquals(List.of(0), split.handed());
        assertEquals(0, split.hold());
        assertEquals(4, split.toStorage());
    }

    @Test
    void severalDependantsInOrder() {
        Handoff.Split split = Handoff.split(5, List.of(new Handoff.Dependant(2, true, false),
                new Handoff.Dependant(0, true, false), new Handoff.Dependant(4, true, false)), 0, 0, 0, 100);
        assertEquals(List.of(2, 0, 3), split.handed());
        assertEquals(5, split.handedTotal());
        assertEquals(0, split.toStorage());
    }

    @Test
    void sameProducerWithRoomBuffersAllOwed() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, true)), 64, 0, 0, 100);
        assertEquals(4, split.buffered());
        assertEquals(0, split.hold());
        assertEquals(0, split.toStorage());
        assertEquals(0, split.overflow());
    }

    @Test
    void sameProducerWithoutRoomGoesToStorage() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, true)), 0, 0, 0, 100);
        assertEquals(0, split.buffered());
        assertEquals(0, split.hold());
        assertEquals(4, split.toStorage());
        assertEquals(4, split.overflow());
    }

    @Test
    void busyProducerWithRoomBuffersWithoutHolding() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, false)), 64, 0, 0, 100);
        assertEquals(List.of(0), split.handed());
        assertEquals(4, split.buffered());
        assertEquals(0, split.hold());
        assertEquals(0, split.toStorage());
    }

    @Test
    void busyProducerPartlyFitsHoldsTheRest() {
        Handoff.Split split = Handoff.split(80, List.of(new Handoff.Dependant(80, false, false)), 64, 0, 20, 100);
        assertEquals(64, split.buffered());
        assertEquals(16, split.hold());
        assertEquals(0, split.toStorage());
        assertEquals(0, split.overflow());
    }

    @Test
    void afterTheWaitTheRestOverflows() {
        Handoff.Split split = Handoff.split(16, List.of(new Handoff.Dependant(80, false, false)), 0, 64, 100, 100);
        assertEquals(0, split.buffered());
        assertEquals(0, split.hold());
        assertEquals(16, split.toStorage());
        assertEquals(16, split.overflow());
    }

    @Test
    void heldItemsLowerWhatIsBuffered() {
        Handoff.Split split = Handoff.split(8, List.of(new Handoff.Dependant(8, false, true)), 64, 6, 0, 100);
        assertEquals(2, split.buffered());
        assertEquals(6, split.toStorage());
        assertEquals(0, split.overflow());
    }

    @Test
    void deliveringDependantIsHandedFirst() {
        Handoff.Split split = Handoff.split(6, List.of(new Handoff.Dependant(4, false, true),
                new Handoff.Dependant(4, true, false)), 64, 0, 0, 100);
        assertEquals(List.of(0, 4), split.handed());
        assertEquals(2, split.buffered());
        assertEquals(0, split.toStorage());
    }

    @Test
    void surplusIsNoOverflow() {
        Handoff.Split split = Handoff.split(10, List.of(new Handoff.Dependant(4, false, true)), 64, 0, 0, 100);
        assertEquals(4, split.buffered());
        assertEquals(6, split.toStorage());
        assertEquals(0, split.overflow());
    }
}
