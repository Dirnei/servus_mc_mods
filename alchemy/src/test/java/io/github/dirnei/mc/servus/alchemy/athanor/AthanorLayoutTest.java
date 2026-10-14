package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.Tank;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.TankSide;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AthanorLayoutTest {

    private static final String SALT = "servus_alchemy:salt_crystal_block";

    @Test
    void hearthIsADiamondOfThirteenBricksBelow() {
        assertEquals(13, AthanorLayout.hearth().size());
        assertTrue(AthanorLayout.hearth().stream().allMatch(block -> Math.abs(block.dx()) + Math.abs(block.dz()) <= 2));
        assertTrue(AthanorLayout.hearth().stream().allMatch(block -> block.dy() == -1 && block.block().equals(AthanorLayout.BRICKS)));
        assertTrue(AthanorLayout.hearth().contains(new BlockAt(0, -1, 0, AthanorLayout.BRICKS)));
    }

    @Test
    void nigredoSchematic() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(3, schematic.layers().size());
        assertEquals(List.of("__B__", "_BBB_", "BB0BB", "_BBB_", "__B__"), schematic.layers().get(2));
        assertEquals(AthanorLayout.COPPER_DUCT, schematic.mapping().get('D'));
        assertEquals(AthanorLayout.BRICKS, schematic.mapping().get('0'));
        assertEquals(AthanorLayout.ATHANOR, schematic.mapping().get('A'));
        assertEquals(AthanorLayout.FLUID_TANK, schematic.mapping().get('T'));
    }

    @Test
    void nigredoSchematicUpperLayersArePaddedToTheHearth() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(List.of("_____", "_____", "DD_DD", "__D__", "__D__"), schematic.layers().get(0));
        assertEquals(List.of("_____", "_____", "T_A_T", "_____", "__T__"), schematic.layers().get(1));
    }

    @Test
    void albedoRingAroundTheHearth() {
        List<BlockAt> ring = new ArrayList<>();
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dz) > 2) {
                    ring.add(new BlockAt(dx, -1, dz, SALT));
                }
            }
        }
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(ring);
        assertEquals(List.of(List.of("_____", "_____", "DD_DD", "__D__", "__D__"), List.of("_____", "_____", "T_A_T", "_____", "__T__"),
                List.of("CCBCC", "CBBBC", "BB0BB", "CBBBC", "CCBCC")), schematic.layers());
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
    void tanksStandOnTheHearth() {
        for (Facing facing : Facing.values()) {
            for (Tank tank : AthanorLayout.tanks(facing)) {
                assertTrue(AthanorLayout.hearth().contains(new BlockAt(tank.dx(), -1, tank.dz(), AthanorLayout.BRICKS)), facing + " " + tank);
            }
        }
    }

    @Test
    void twoDuctsPerTankTheFarOneOverIt() {
        for (Facing facing : Facing.values()) {
            List<AthanorLayout.Duct> ducts = AthanorLayout.ducts(facing);
            assertEquals(6, ducts.size());
            for (Tank tank : AthanorLayout.tanks(facing)) {
                assertTrue(ducts.contains(new AthanorLayout.Duct(tank.side(), true, tank.dx(), tank.dz())), facing + " " + tank);
                assertTrue(ducts.contains(new AthanorLayout.Duct(tank.side(), false, tank.dx() / 2, tank.dz() / 2)), facing + " " + tank);
            }
        }
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
