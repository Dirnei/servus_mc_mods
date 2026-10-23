package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.TierDef;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AthanorTiersTest {

    private static final String SALT = "s:salt_crystal_block";
    private static final String SULFUR = "s:sulfur_pillar";

    private static List<BlockAt> ring(String block) {
        List<BlockAt> ring = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dz != 0) {
                    ring.add(new BlockAt(dx, 0, dz, block));
                }
            }
        }
        return ring;
    }

    private static final TierDef ALBEDO = new TierDef("albedo", 1, 0xFFFFFF, ring(SALT));
    private static final TierDef CITRINITAS = new TierDef("citrinitas", 2, 0xFFDD00,
            List.of(new BlockAt(2, 0, 2, SULFUR), new BlockAt(-2, 0, -2, SULFUR)));

    private static Map<String, String> world(List<BlockAt> placed) {
        Map<String, String> world = new HashMap<>();
        placed.forEach(block -> world.put(block.dx() + "," + block.dy() + "," + block.dz(), block.block()));
        return world;
    }

    private static Function<BlockAt, String> at(Map<String, String> world) {
        return block -> world.get(block.dx() + "," + block.dy() + "," + block.dz());
    }

    private static int tier(List<TierDef> tiers, Map<String, String> world) {
        return AthanorTiers.tierOf(tiers, Facing.NORTH, at(world));
    }

    private static final String AMETHYST = "minecraft:amethyst_block";
    private static final String CRYING = "minecraft:crying_obsidian";

    @Test
    void nothingAroundIsNigredo() {
        assertEquals(0, tier(List.of(ALBEDO, CITRINITAS), world(List.of())));
        assertEquals(0, tier(List.of(), world(ring(SALT))), "no tier definitions");
    }

    @Test
    void fullRingIsAlbedo() {
        assertEquals(1, tier(List.of(ALBEDO, CITRINITAS), world(ring(SALT))));
        assertEquals("albedo", AthanorTiers.nameOf(List.of(ALBEDO), 1));
        assertEquals(AthanorTiers.NIGREDO, AthanorTiers.nameOf(List.of(ALBEDO), 0));
    }

    @Test
    void missingLowerTierBlocksHigherTiers() {
        List<BlockAt> placed = new ArrayList<>(ring(SALT).subList(1, 8));
        placed.addAll(CITRINITAS.blocks());
        assertEquals(0, tier(List.of(CITRINITAS, ALBEDO), world(placed)), "a gap in the ring stops at Nigredo");
        placed.add(ring(SALT).get(0));
        assertEquals(2, tier(List.of(CITRINITAS, ALBEDO), world(placed)));
    }

    @Test
    void wrongBlockDoesNotCount() {
        List<BlockAt> placed = new ArrayList<>(ring(SALT));
        placed.set(3, new BlockAt(placed.get(3).dx(), 0, placed.get(3).dz(), "minecraft:stone"));
        assertEquals(0, tier(List.of(ALBEDO), world(placed)));
    }

    private static final List<TierDef> STAGES = List.of(
            new TierDef("albedo", 1, 0xF2F0EA, AthanorLayoutTest.saltRing()),
            new TierDef("citrinitas", 2, 0xF2C531, AthanorLayoutTest.vats()),
            new TierDef("rubedo", 3, 0xB3261E, AthanorLayoutTest.pillars()));

    private static List<BlockAt> turned(List<BlockAt> blocks, Facing facing) {
        return blocks.stream().map(block -> AthanorTiers.turn(block, facing)).toList();
    }

    @Test
    void offsetsTurnWithTheFront() {
        BlockAt leftBehind = new BlockAt(1, -1, 2, CRYING);
        assertEquals(leftBehind, AthanorTiers.turn(leftBehind, Facing.NORTH));
        assertEquals(new BlockAt(-2, -1, 1, CRYING), AthanorTiers.turn(leftBehind, Facing.EAST), "left is south, behind is west");
        assertEquals(new BlockAt(-1, -1, -2, CRYING), AthanorTiers.turn(leftBehind, Facing.SOUTH));
        assertEquals(new BlockAt(2, -1, -1, CRYING), AthanorTiers.turn(leftBehind, Facing.WEST));
    }

    @Test
    void eastFacingReachesRubedo() {
        List<BlockAt> placed = new ArrayList<>();
        STAGES.forEach(stage -> placed.addAll(turned(stage.blocks(), Facing.EAST)));
        assertEquals(3, AthanorTiers.tierOf(STAGES, Facing.EAST, at(world(placed))));
        assertEquals(1, AthanorTiers.tierOf(STAGES, Facing.NORTH, at(world(placed))), "the symmetric ring alone fits a north front");
        assertEquals("rubedo", AthanorTiers.nameOf(STAGES, 3));
    }

    @Test
    void vatsWithoutSaltStayNigredo() {
        assertEquals(0, tier(STAGES, world(AthanorLayoutTest.vats())), "vats without the salt ring are Nigredo");
        List<BlockAt> placed = new ArrayList<>(AthanorLayoutTest.saltRing());
        placed.addAll(AthanorLayoutTest.vats());
        assertEquals(2, tier(STAGES, world(placed)));
        placed.removeIf(block -> block.block().equals(AMETHYST) && block.dz() == 3);
        assertEquals(1, tier(STAGES, world(placed)), "a vat without its amethyst is Albedo");
    }

    @Test
    void shortPillarDropsToCitrinitas() {
        List<BlockAt> placed = new ArrayList<>(AthanorLayoutTest.saltRing());
        placed.addAll(AthanorLayoutTest.vats());
        placed.addAll(AthanorLayoutTest.pillars());
        assertEquals(3, tier(STAGES, world(placed)));
        placed.remove(new BlockAt(2, 0, 1, CRYING));
        assertEquals(2, tier(STAGES, world(placed)));
    }

    @Test
    void tierNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new TierDef("bad", 0, 0, List.of()));
    }
}
