package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.Duct;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.Tank;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorLayout.TankSide;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AthanorLayoutTest {

    private static final String SALT = "servus_alchemy:salt_crystal_block";
    private static final String VAT = "servus_alchemy:principle_vat";
    private static final String CRYING = "minecraft:crying_obsidian";

    /** The sixteen cells of the 5 by 5 ring one layer under the hearth. */
    static List<BlockAt> saltRing() {
        List<BlockAt> ring = new ArrayList<>();
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2) {
                    ring.add(new BlockAt(dx, -2, dz, SALT));
                }
            }
        }
        return ring;
    }

    /** The three vats with grown citrine beside the tanks. */
    static List<BlockAt> vats() {
        List<BlockAt> vats = new ArrayList<>();
        for (int[] cell : new int[][] {{0, 3}, {-3, 0}, {3, 0}}) {
            vats.add(new BlockAt(cell[0], -1, cell[1], VAT, Map.of("crystal", "citrine")));
        }
        return vats;
    }

    static List<BlockAt> pillars() {
        List<BlockAt> pillars = new ArrayList<>();
        for (int dy = -1; dy <= 0; dy++) {
            for (int[] cell : new int[][] {{-1, 2}, {1, 2}, {-2, 1}, {2, 1}}) {
                pillars.add(new BlockAt(cell[0], dy, cell[1], CRYING));
            }
        }
        return pillars;
    }

    @Test
    void hearthRolesTurnWithTheFront() {
        List<BlockAt> north = AthanorLayout.hearth(Facing.NORTH);
        assertEquals(9, north.size());
        assertTrue(north.contains(new BlockAt(0, -1, 0, AthanorLayout.BRICKS)), "Bricks under the Athanor");
        assertTrue(north.contains(new BlockAt(0, -1, -1, AthanorLayout.BRICKS)), "Bricks in front");
        assertEquals(7, north.stream().filter(block -> block.block().equals(AthanorLayout.BRICK_SLAB)).count());
        assertTrue(north.contains(new BlockAt(0, -1, 1, AthanorLayout.BRICK_SLAB)), "a slab behind");
        assertEquals(new BlockAt(0, -1, -2, AthanorLayout.BRICK_STAIRS), AthanorLayout.step(Facing.NORTH));

        List<BlockAt> east = AthanorLayout.hearth(Facing.EAST);
        assertTrue(east.contains(new BlockAt(1, -1, 0, AthanorLayout.BRICKS)), "Bricks in front of an east facing Athanor");
        assertTrue(east.contains(new BlockAt(-1, -1, 0, AthanorLayout.BRICK_SLAB)), "a slab behind it");
        assertEquals(new BlockAt(2, -1, 0, AthanorLayout.BRICK_STAIRS), AthanorLayout.step(Facing.EAST));
        assertEquals(new BlockAt(0, -1, 2, AthanorLayout.BRICK_STAIRS), AthanorLayout.step(Facing.SOUTH));
        for (Facing facing : Facing.values()) {
            assertTrue(AthanorLayout.hearth(facing).stream().allMatch(block -> Math.abs(block.dx()) <= 1 && Math.abs(block.dz()) <= 1),
                    facing + ": the hearth is 3 by 3 under the Athanor");
        }
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
    void tanksAreThreeHigh() {
        List<BlockAt> blocks = AthanorLayout.tankBlocks(Facing.NORTH);
        assertEquals(9, blocks.size());
        for (Tank tank : AthanorLayout.tanks(Facing.NORTH)) {
            for (int dy = -1; dy <= 1; dy++) {
                assertTrue(blocks.contains(new BlockAt(tank.dx(), dy, tank.dz(), AthanorLayout.FLUID_TANK)), tank + " at " + dy);
            }
        }
    }

    @Test
    void oneDuctPerTankOnTheAthanorLevel() {
        for (Facing facing : Facing.values()) {
            List<Duct> ducts = AthanorLayout.ducts(facing);
            assertEquals(3, ducts.size());
            for (Tank tank : AthanorLayout.tanks(facing)) {
                assertTrue(ducts.contains(new Duct(tank.side(), tank.dx() / 2, tank.dz() / 2)), facing + " " + tank);
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

    /** Rows run along x from west to east, each row along z from the front to the back, as Modonomicon reads them. */
    @Test
    void nigredoSchematicLayers() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(List.of());
        assertEquals(List.of(
                List.of("__T__", "_____", "____T", "_____", "__T__"),
                List.of("__T__", "__D__", "__ADT", "__D__", "__T__"),
                List.of("__T__", "_SSS_", "RB0ST", "_SSS_", "__T__")), schematic.layers());
        assertEquals(AthanorLayout.BRICKS, schematic.mapping().get('0'));
        assertEquals(AthanorLayout.BRICKS, schematic.mapping().get('B'));
        assertEquals("minecraft:brick_slab[type=bottom]", schematic.mapping().get('S'));
        assertEquals("minecraft:brick_stairs[facing=south]", schematic.mapping().get('R'));
        assertEquals(AthanorLayout.ATHANOR, schematic.mapping().get('A'));
        assertEquals(AthanorLayout.FLUID_TANK, schematic.mapping().get('T'));
        assertEquals(AthanorLayout.COPPER_DUCT, schematic.mapping().get('D'));
    }

    @Test
    void eachVatBelongsToItsTank() {
        assertEquals(TankSide.BACK, AthanorLayout.nearestTank(Facing.NORTH, 0, 3).side());
        assertEquals(TankSide.LEFT, AthanorLayout.nearestTank(Facing.NORTH, 3, 0).side());
        assertEquals(TankSide.RIGHT, AthanorLayout.nearestTank(Facing.NORTH, -3, 0).side());
        BlockAt behind = AthanorTiers.turn(new BlockAt(0, -1, 3, VAT), Facing.EAST);
        assertEquals(TankSide.BACK, AthanorLayout.nearestTank(Facing.EAST, behind.dx(), behind.dz()).side(), "turned with the front");
    }

    @Test
    void albedoSchematicHasTheRingBelow() {
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(saltRing());
        assertEquals(4, schematic.layers().size());
        assertEquals(List.of("CCCCC", "C___C", "C_0_C", "C___C", "CCCCC"), schematic.layers().get(3));
        assertEquals(List.of("__T__", "_SSS_", "RBBST", "_SSS_", "__T__"), schematic.layers().get(2));
        assertEquals(SALT, schematic.mapping().get('C'));
        assertEquals(AthanorLayout.ANY, schematic.mapping().get('0'), "the ring is hollow, so the anchor takes any block");
    }

    @Test
    void rubedoSchematicHoldsVatsAndPillars() {
        List<BlockAt> extra = new ArrayList<>(saltRing());
        extra.addAll(vats());
        extra.addAll(pillars());
        AthanorLayout.Schematic schematic = AthanorLayout.schematic(extra);
        assertEquals(List.of(
                List.of("______", "__T___", "______", "____T_", "______", "__T___", "______"),
                List.of("______", "__TF__", "__D_F_", "__ADT_", "__D_F_", "__TF__", "______"),
                List.of("__E___", "__TF__", "_SSSF_", "RBBSTE", "_SSSF_", "__TF__", "__E___"),
                List.of("______", "CCCCC_", "C___C_", "C_0_C_", "C___C_", "CCCCC_", "______")),
                schematic.layers());
        assertEquals(VAT, schematic.mapping().get('E'));
        assertEquals(CRYING, schematic.mapping().get('F'));
    }
}
