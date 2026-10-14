package io.github.dirnei.mc.servus.alchemy.athanor;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Athanor's build: a diamond hearth of 13 Bricks right below it, three Fluid Tanks two blocks
 * out behind it and to its left and right, standing on the tips of the diamond, two Copper Ducts
 * per tank one block up, and the schematic of a stage, the hearth, the Athanor
 * and its tanks with the blocks of its tiers around. Pure Java.
 */
public final class AthanorLayout {

    public static final String BRICKS = "minecraft:bricks";
    public static final String ATHANOR = "servus_alchemy:athanor";
    public static final String FLUID_TANK = "servus_foundation:fluid_tank";
    public static final String COPPER_DUCT = "servus_alchemy:copper_duct";

    /** Where a tank stands, seen from the Athanor's front. */
    public enum TankSide {
        LEFT, BACK, RIGHT
    }

    /** A tank position. */
    public record Tank(TankSide side, int dx, int dz) {
    }

    /**
     * A Copper Duct of the build, one block above the Athanor's level.
     *
     * @param overTank true for the duct over the tank, which turns down into it; false for the one beside the Athanor
     */
    public record Duct(TankSide side, boolean overTank, int dx, int dz) {
    }

    /** The hearth: 13 Bricks in a diamond reaching two blocks out, one layer below the Athanor. */
    private static final List<BlockAt> HEARTH;

    static {
        List<BlockAt> hearth = new ArrayList<>();
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (Math.abs(dx) + Math.abs(dz) <= 2) {
                    hearth.add(new BlockAt(dx, -1, dz, BRICKS));
                }
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

    /**
     * The three tanks of an Athanor whose front faces {@code facing}, two blocks out on its level.
     * None touches another, so Fluid Tanks there never merge.
     */
    public static List<Tank> tanks(Facing facing) {
        int rx = facing.rightX();
        int rz = facing.rightZ();
        return List.of(
                new Tank(TankSide.LEFT, -2 * rx, -2 * rz),
                new Tank(TankSide.BACK, 2 * rz, -2 * rx),
                new Tank(TankSide.RIGHT, 2 * rx, 2 * rz));
    }

    /**
     * The six ducts of an Athanor whose front faces {@code facing}: for each tank one beside the
     * Athanor and one over the tank, one block up.
     */
    public static List<Duct> ducts(Facing facing) {
        List<Duct> ducts = new ArrayList<>();
        for (Tank tank : tanks(facing)) {
            int ux = Integer.signum(tank.dx());
            int uz = Integer.signum(tank.dz());
            ducts.add(new Duct(tank.side(), false, ux, uz));
            ducts.add(new Duct(tank.side(), true, tank.dx(), tank.dz()));
        }
        return List.copyOf(ducts);
    }

    /** The schematic of the hearth, the Athanor facing north with its tanks and ducts, and {@code extra} blocks around it. */
    public static Schematic schematic(List<BlockAt> extra) {
        List<BlockAt> all = new ArrayList<>(HEARTH);
        all.add(new BlockAt(0, 0, 0, ATHANOR));
        tanks(Facing.NORTH).forEach(tank -> all.add(new BlockAt(tank.dx(), 0, tank.dz(), FLUID_TANK)));
        ducts(Facing.NORTH).forEach(duct -> all.add(new BlockAt(duct.dx(), 1, duct.dz(), COPPER_DUCT)));
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
        mapping.put('T', FLUID_TANK);
        mapping.put('D', COPPER_DUCT);
        Map<String, Character> symbols = new LinkedHashMap<>();
        char next = 'C';
        for (BlockAt block : extra) {
            if (!symbols.containsKey(block.block())) {
                symbols.put(block.block(), next);
                mapping.put(next, block.block());
                next++;
                if (next == 'D') {
                    next++;
                }
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
                if (block.block().equals(FLUID_TANK)) {
                    return 'T';
                }
                if (block.block().equals(COPPER_DUCT)) {
                    return 'D';
                }
                Character symbol = symbols.get(block.block());
                return symbol != null ? symbol : 'B';
            }
        }
        return '_';
    }
}
