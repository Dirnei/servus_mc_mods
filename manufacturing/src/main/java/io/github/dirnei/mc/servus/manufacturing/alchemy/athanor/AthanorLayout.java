package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorTiers.BlockAt;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Athanor's build: a hearth of 3 by 3 Bricks right below it, and the schematic of a stage,
 * the hearth and the Athanor with the blocks of its tiers around. Pure Java.
 */
public final class AthanorLayout {

    public static final String BRICKS = "minecraft:bricks";
    public static final String ATHANOR = "servus_manufacturing:athanor";

    /** The hearth: nine Bricks, one layer below the Athanor. */
    private static final List<BlockAt> HEARTH;

    static {
        List<BlockAt> hearth = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                hearth.add(new BlockAt(dx, -1, dz, BRICKS));
            }
        }
        HEARTH = List.copyOf(hearth);
    }

    /**
     * A dense schematic: layers top to bottom, each a list of rows along z, each row a string
     * along x, and the block id of every character. {@code 0} is the hearth centre, the anchor.
     */
    public record Schematic(List<List<String>> layers, Map<Character, String> mapping) {
    }

    private AthanorLayout() {
    }

    public static List<BlockAt> hearth() {
        return HEARTH;
    }

    /** The schematic of the hearth, the Athanor and {@code extra} blocks around it. */
    public static Schematic schematic(List<BlockAt> extra) {
        List<BlockAt> all = new ArrayList<>(HEARTH);
        all.add(new BlockAt(0, 0, 0, ATHANOR));
        all.addAll(extra);
        int minX = all.stream().mapToInt(BlockAt::dx).min().orElse(0);
        int maxX = all.stream().mapToInt(BlockAt::dx).max().orElse(0);
        int minY = all.stream().mapToInt(BlockAt::dy).min().orElse(0);
        int maxY = all.stream().mapToInt(BlockAt::dy).max().orElse(0);
        int minZ = all.stream().mapToInt(BlockAt::dz).min().orElse(0);
        int maxZ = all.stream().mapToInt(BlockAt::dz).max().orElse(0);

        Map<Character, String> mapping = new LinkedHashMap<>();
        mapping.put('0', BRICKS);
        mapping.put('B', BRICKS);
        mapping.put('A', ATHANOR);
        Map<String, Character> symbols = new LinkedHashMap<>();
        char next = 'C';
        for (BlockAt block : extra) {
            if (!symbols.containsKey(block.block())) {
                symbols.put(block.block(), next);
                mapping.put(next, block.block());
                next++;
            }
        }

        List<List<String>> layers = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            List<String> rows = new ArrayList<>();
            for (int z = minZ; z <= maxZ; z++) {
                StringBuilder row = new StringBuilder();
                for (int x = minX; x <= maxX; x++) {
                    row.append(symbolAt(all, symbols, x, y, z));
                }
                rows.add(row.toString());
            }
            layers.add(rows);
        }
        return new Schematic(layers, mapping);
    }

    private static char symbolAt(List<BlockAt> all, Map<String, Character> symbols, int x, int y, int z) {
        if (x == 0 && y == -1 && z == 0) {
            return '0';
        }
        for (BlockAt block : all) {
            if (block.dx() == x && block.dy() == y && block.dz() == z) {
                if (block.block().equals(ATHANOR)) {
                    return 'A';
                }
                Character symbol = symbols.get(block.block());
                return symbol != null ? symbol : 'B';
            }
        }
        return '_';
    }
}
