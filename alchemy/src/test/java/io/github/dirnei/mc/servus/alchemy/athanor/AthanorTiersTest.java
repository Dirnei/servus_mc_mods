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

    @Test
    void tierNumbersStartAtOne() {
        assertThrows(IllegalArgumentException.class, () -> new TierDef("bad", 0, 0, List.of()));
    }
}
