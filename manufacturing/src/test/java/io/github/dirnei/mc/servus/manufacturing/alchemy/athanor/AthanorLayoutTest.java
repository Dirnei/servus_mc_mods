package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout.Tank;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout.TankSide;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Facing;
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
        assertEquals(List.of("_BBB_", "_B0B_", "_BBB_", "_____"), schematic.layers().get(1));
        assertEquals(AthanorLayout.BRICKS, schematic.mapping().get('0'));
        assertEquals(AthanorLayout.ATHANOR, schematic.mapping().get('A'));
        assertEquals(AthanorLayout.FLUID_TANK, schematic.mapping().get('T'));
    }

    @Test
    void nigredoSchematicTopLayerIsPaddedToTheHearth() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(List.of("_____", "T_A_T", "_____", "__T__"), schematic.layers().getFirst());
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
        assertEquals(List.of(List.of("_CCC_", "TCACT", "_CCC_", "__T__"), List.of("_BBB_", "_B0B_", "_BBB_", "_____")), schematic.layers());
        assertEquals(SALT, schematic.mapping().get('C'));
    }

    @Test
    void tanksFacingNorth() {
        assertEquals(List.of(new Tank(TankSide.LEFT, 2, 0), new Tank(TankSide.BACK, 0, 2), new Tank(TankSide.RIGHT, -2, 0)),
                AthanorLayout.tanks(Facing.NORTH));
    }

    @Test
    void tanksTurnWithTheFront() {
        assertEquals(List.of(new Tank(TankSide.LEFT, 0, 2), new Tank(TankSide.BACK, -2, 0), new Tank(TankSide.RIGHT, 0, -2)),
                AthanorLayout.tanks(Facing.EAST));
    }

    @Test
    void tanksNeverTouch() {
        for (Facing facing : Facing.values()) {
            List<Tank> tanks = AthanorLayout.tanks(facing);
            for (Tank a : tanks) {
                for (Tank b : tanks) {
                    if (a != b) {
                        assertTrue(Math.abs(a.dx() - b.dx()) + Math.abs(a.dz() - b.dz()) > 1, facing + " " + a + " " + b);
                    }
                }
            }
        }
    }
}
