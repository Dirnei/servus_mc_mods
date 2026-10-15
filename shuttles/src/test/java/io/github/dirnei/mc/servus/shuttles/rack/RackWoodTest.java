package io.github.dirnei.mc.servus.shuttles.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class RackWoodTest {

    @Test
    void threeEqualPicksThatWood() {
        assertEquals("spruce", RackWood.pick(List.of("spruce", "spruce", "spruce")));
    }

    @Test
    void twoToOnePicksTheMajority() {
        assertEquals("oak", RackWood.pick(List.of("oak", "spruce", "oak")));
        assertEquals("oak", RackWood.pick(List.of("spruce", "oak", "oak")));
        assertEquals("oak", RackWood.pick(List.of("oak", "oak", "spruce")));
    }

    @Test
    void threeDifferentPicksTheMiddle() {
        assertEquals("spruce", RackWood.pick(List.of("birch", "spruce", "oak")));
    }

    @Test
    void singleEntryPicksIt() {
        assertEquals("birch", RackWood.pick(List.of("birch")));
    }

    @Test
    void emptyListPicksNothing() {
        assertNull(RackWood.pick(List.of()));
    }
}
