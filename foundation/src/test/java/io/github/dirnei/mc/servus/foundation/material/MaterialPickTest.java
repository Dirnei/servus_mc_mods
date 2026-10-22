package io.github.dirnei.mc.servus.foundation.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialPickTest {

    @Test
    void threeEqualPicksThatWood() {
        assertEquals("spruce", MaterialPick.pick(List.of("spruce", "spruce", "spruce")));
    }

    @Test
    void twoToOnePicksTheMajority() {
        assertEquals("oak", MaterialPick.pick(List.of("oak", "spruce", "oak")));
        assertEquals("oak", MaterialPick.pick(List.of("spruce", "oak", "oak")));
        assertEquals("oak", MaterialPick.pick(List.of("oak", "oak", "spruce")));
    }

    @Test
    void threeDifferentPicksTheMiddle() {
        assertEquals("spruce", MaterialPick.pick(List.of("birch", "spruce", "oak")));
    }

    @Test
    void singleEntryPicksIt() {
        assertEquals("birch", MaterialPick.pick(List.of("birch")));
    }

    @Test
    void stoneIdsFollowTheSameRule() {
        assertEquals("minecraft:stone_bricks",
                MaterialPick.pick(List.of("minecraft:stone_bricks", "minecraft:bricks", "minecraft:stone_bricks")));
    }

    @Test
    void emptyListPicksNothing() {
        assertNull(MaterialPick.pick(List.of()));
    }
}
