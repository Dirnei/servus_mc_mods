package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.BlockAt;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AthanorLayoutTest {

    private static final String SALT = "servus_manufacturing:salt_crystal_block";

    @Test
    void hearthIsNineBricksBelow() {
        assertEquals(9, AthanorLayout.hearth().size());
        assertTrue(AthanorLayout.hearth().stream().allMatch(block -> block.dy() == -1 && block.block().equals(AthanorLayout.BRICKS)));
        assertTrue(AthanorLayout.hearth().contains(new BlockAt(0, -1, 0, AthanorLayout.BRICKS)));
    }

    @Test
    void nigredoSchematic() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(2, schematic.layers().size());
        assertEquals(List.of("BBB", "B0B", "BBB"), schematic.layers().get(1));
        assertEquals(AthanorLayout.BRICKS, schematic.mapping().get('0'));
        assertEquals(AthanorLayout.ATHANOR, schematic.mapping().get('A'));
    }

    @Test
    void nigredoSchematicTopLayerIsPaddedToTheHearth() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(List.of("___", "_A_", "___"), schematic.layers().getFirst());
    }

    @Test
    void albedoRingAroundTheAthanor() {
        List<BlockAt> ring = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dz != 0) {
                    ring.add(new BlockAt(dx, 0, dz, SALT));
                }
            }
        }
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(ring);
        assertEquals(List.of(List.of("CCC", "CAC", "CCC"), List.of("BBB", "B0B", "BBB")), schematic.layers());
        assertEquals(SALT, schematic.mapping().get('C'));
    }
}
