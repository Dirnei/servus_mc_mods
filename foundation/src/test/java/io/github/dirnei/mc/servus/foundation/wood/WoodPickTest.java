package io.github.dirnei.mc.servus.foundation.wood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class WoodPickTest {

    @Test
    void threeEqualPicksThatWood() {
        assertEquals("spruce", WoodPick.pick(List.of("spruce", "spruce", "spruce")));
    }

    @Test
    void twoToOnePicksTheMajority() {
        assertEquals("oak", WoodPick.pick(List.of("oak", "spruce", "oak")));
        assertEquals("oak", WoodPick.pick(List.of("spruce", "oak", "oak")));
        assertEquals("oak", WoodPick.pick(List.of("oak", "oak", "spruce")));
    }

    @Test
    void threeDifferentPicksTheMiddle() {
        assertEquals("spruce", WoodPick.pick(List.of("birch", "spruce", "oak")));
    }

    @Test
    void singleEntryPicksIt() {
        assertEquals("birch", WoodPick.pick(List.of("birch")));
    }

    @Test
    void emptyListPicksNothing() {
        assertNull(WoodPick.pick(List.of()));
    }
}
