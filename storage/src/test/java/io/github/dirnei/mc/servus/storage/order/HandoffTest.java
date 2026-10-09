package io.github.dirnei.mc.servus.storage.order;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class HandoffTest {

    @Test
    void allToDeliveringDependant() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, true, false)), 0, 100);
        assertEquals(List.of(4), split.handed());
        assertEquals(0, split.hold());
        assertEquals(0, split.toStorage());
    }

    @Test
    void surplusGoesToStorage() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(2, true, false)), 0, 100);
        assertEquals(List.of(2), split.handed());
        assertEquals(0, split.hold());
        assertEquals(2, split.toStorage());
    }

    @Test
    void holdsWhileDependantWaits() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(3, false, false)), 40, 100);
        assertEquals(List.of(0), split.handed());
        assertEquals(3, split.hold());
        assertEquals(1, split.toStorage());
    }

    @Test
    void holdExpires() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, false)), 100, 100);
        assertEquals(0, split.hold());
        assertEquals(4, split.toStorage());
    }

    @Test
    void sameProducerGoesToStorage() {
        Handoff.Split split = Handoff.split(4, List.of(new Handoff.Dependant(4, false, true)), 0, 100);
        assertEquals(List.of(0), split.handed());
        assertEquals(0, split.hold());
        assertEquals(4, split.toStorage());
    }

    @Test
    void severalDependantsInOrder() {
        Handoff.Split split = Handoff.split(5, List.of(new Handoff.Dependant(2, true, false),
                new Handoff.Dependant(0, true, false), new Handoff.Dependant(4, true, false)), 0, 100);
        assertEquals(List.of(2, 0, 3), split.handed());
        assertEquals(5, split.handedTotal());
        assertEquals(0, split.toStorage());
    }
}
