package io.github.dirnei.mc.servus.alchemy.athanor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.TierDef;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static int tier(List<TierDef> tiers, Map<String, String> world) {
        return AthanorTiers.tierOf(tiers, block -> world.get(block.dx() + "," + block.dy() + "," + block.dz()));
    }

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

    private static final String AMETHYST = "minecraft:amethyst_block";
    private static final String CRYING = "minecraft:crying_obsidian";

    /** The twelve cells of the 5 by 5 square outside the diamond hearth, at height {@code dy}. */
    private static List<BlockAt> squareCorners(int dy, String block) {
        List<BlockAt> cells = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > 2) {
                    cells.add(new BlockAt(dx, dy, dz, block));
                }
            }
        }
        return cells;
    }

    private static List<BlockAt> pillars() {
        List<BlockAt> pillars = new ArrayList<>();
        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx += 4) {
                for (int dz = -2; dz <= 2; dz += 4) {
                    pillars.add(new BlockAt(dx, dy, dz, CRYING));
                }
            }
        }
        return pillars;
    }

    private static final List<TierDef> STAGES = List.of(
            new TierDef("albedo", 1, 0xF2F0EA, squareCorners(-1, SALT)),
            new TierDef("citrinitas", 2, 0xF2C531, squareCorners(0, AMETHYST)),
            new TierDef("rubedo", 3, 0xB3261E, pillars()));

    @Test
    void citrinitasNeedsAlbedo() {
        assertEquals(0, tier(STAGES, world(squareCorners(0, AMETHYST))), "amethyst without the salt ring is Nigredo");
        List<BlockAt> placed = new ArrayList<>(squareCorners(-1, SALT));
        placed.addAll(squareCorners(0, AMETHYST));
        assertEquals(2, tier(STAGES, world(placed)));
        assertEquals("citrinitas", AthanorTiers.nameOf(STAGES, 2));
        placed.remove(placed.size() - 1);
        assertEquals(1, tier(STAGES, world(placed)), "a missing amethyst is Albedo");
    }

    @Test
    void rubedoOnTopOfCitrinitas() {
        List<BlockAt> placed = new ArrayList<>(squareCorners(-1, SALT));
        placed.addAll(squareCorners(0, AMETHYST));
        placed.addAll(pillars());
        assertEquals(3, tier(STAGES, world(placed)));
        assertEquals("rubedo", AthanorTiers.nameOf(STAGES, 3));
    }

    @Test
    void shortPillarIsCitrinitas() {
        List<BlockAt> placed = new ArrayList<>(squareCorners(-1, SALT));
        placed.addAll(squareCorners(0, AMETHYST));
        placed.addAll(pillars().stream().filter(block -> !(block.dy() == 2 && block.dx() == 2 && block.dz() == 2)).toList());
        assertEquals(2, tier(STAGES, world(placed)));
    }

    @Test
    void tierNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new TierDef("bad", 0, 0, List.of()));
    }
}
