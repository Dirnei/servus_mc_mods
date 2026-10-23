package io.github.dirnei.mc.servus.alchemy.athanor;

import io.github.dirnei.mc.servus.alchemy.athanor.AthanorTiers.BlockAt;
import io.github.dirnei.mc.servus.alchemy.structure.Facing;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Athanor's build: a 3 by 3 hearth right below it (Bricks under it and in front, Brick Slabs on
 * the other cells) with a Brick Stair as the step in front, three Fluid Tanks three high two blocks
 * out behind it and to its left and right, one Copper Duct per tank on the Athanor's level, and the
 * schematic of a stage with the blocks of its tiers around. Pure Java.
 */
public final class AthanorLayout {

    public static final String BRICKS = "minecraft:bricks";
    public static final String BRICK_SLAB = "minecraft:brick_slab";
    public static final String BRICK_STAIRS = "minecraft:brick_stairs";
    public static final String ATHANOR = "servus_alchemy:athanor";
    public static final String FLUID_TANK = "servus_foundation:fluid_tank";
    public static final String COPPER_DUCT = "servus_alchemy:copper_duct";
    /** In a schematic mapping: any block, for an anchor cell the build leaves open. */
    public static final String ANY = "*";
    /** The lowest and highest level of a tank column, relative to the Athanor. */
    public static final int TANK_BOTTOM = -1;
    public static final int TANK_TOP = 1;

    /** Where a tank stands, seen from the Athanor's front. */
    public enum TankSide {
        LEFT, BACK, RIGHT
    }

    /** A tank column, standing from {@link #TANK_BOTTOM} to {@link #TANK_TOP}. */
    public record Tank(TankSide side, int dx, int dz) {
    }

    /** The Copper Duct on the Athanor's level between the Athanor and the tank on {@code side}. */
    public record Duct(TankSide side, int dx, int dz) {
    }

    /**
     * A dense schematic in Modonomicon's order: layers top to bottom, each a list of rows along x from west to
     * east, each row a string along z from the front (north) to the back, and what every character stands for: a block id, optionally with block state properties
     * such as {@code minecraft:brick_slab[type=bottom]}, or {@link #ANY}. {@code 0}, the anchor, is the middle
     * of the bottom layer.
     */
    public record Schematic(List<List<String>> layers, Map<Character, String> mapping) {
    }

    private AthanorLayout() {
    }

    /** The nine hearth cells below an Athanor facing {@code facing}: Bricks under it and in front, Brick Slabs around. */
    public static List<BlockAt> hearth(Facing facing) {
        List<BlockAt> hearth = new ArrayList<>();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                boolean brick = dx == 0 && dz <= 0;
                hearth.add(AthanorTiers.turn(new BlockAt(dx, -1, dz, brick ? BRICKS : BRICK_SLAB), facing));
            }
        }
        return List.copyOf(hearth);
    }

    /** The Brick Stair on the hearth level in front of the front Brick. */
    public static BlockAt step(Facing facing) {
        return AthanorTiers.turn(new BlockAt(0, -1, -2, BRICK_STAIRS), facing);
    }

    /**
     * The three tanks of an Athanor whose front faces {@code facing}, two blocks out.
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

    /** The tank whose side of the Athanor the horizontal offset {@code dx, dz} points to most. */
    public static Tank nearestTank(Facing facing, int dx, int dz) {
        Tank nearest = null;
        long best = Long.MIN_VALUE;
        for (Tank tank : tanks(facing)) {
            long toward = (long) tank.dx() * dx + (long) tank.dz() * dz;
            if (toward > best) {
                best = toward;
                nearest = tank;
            }
        }
        return nearest;
    }

    /** Every Fluid Tank block of the three tank columns. */
    public static List<BlockAt> tankBlocks(Facing facing) {
        List<BlockAt> blocks = new ArrayList<>();
        for (Tank tank : tanks(facing)) {
            for (int dy = TANK_BOTTOM; dy <= TANK_TOP; dy++) {
                blocks.add(new BlockAt(tank.dx(), dy, tank.dz(), FLUID_TANK));
            }
        }
        return List.copyOf(blocks);
    }

    /** The three ducts of an Athanor whose front faces {@code facing}, one beside it toward each tank. */
    public static List<Duct> ducts(Facing facing) {
        return tanks(facing).stream()
                .map(tank -> new Duct(tank.side(), Integer.signum(tank.dx()), Integer.signum(tank.dz())))
                .toList();
    }

    /** The schematic of the hearth, the Athanor facing north with its tanks and ducts, and {@code extra} blocks around it. */
    public static Schematic schematic(List<BlockAt> extra) {
        Map<Character, String> mapping = new LinkedHashMap<>();
        mapping.put('B', BRICKS);
        mapping.put('S', BRICK_SLAB + "[type=bottom]");
        mapping.put('R', BRICK_STAIRS + "[facing=south]");
        mapping.put('A', ATHANOR);
        mapping.put('T', FLUID_TANK);
        mapping.put('D', COPPER_DUCT);
        Map<String, Character> symbols = new LinkedHashMap<>();
        symbols.put(BRICKS, 'B');
        symbols.put(BRICK_SLAB, 'S');
        symbols.put(BRICK_STAIRS, 'R');
        symbols.put(ATHANOR, 'A');
        symbols.put(FLUID_TANK, 'T');
        symbols.put(COPPER_DUCT, 'D');
        char next = 'C';
        for (BlockAt block : extra) {
            if (!symbols.containsKey(block.block())) {
                while (mapping.containsKey(next)) {
                    next++;
                }
                symbols.put(block.block(), next);
                mapping.put(next, block.block());
            }
        }

        List<BlockAt> all = new ArrayList<>(hearth(Facing.NORTH));
        all.add(step(Facing.NORTH));
        all.add(new BlockAt(0, 0, 0, ATHANOR));
        all.addAll(tankBlocks(Facing.NORTH));
        ducts(Facing.NORTH).forEach(duct -> all.add(new BlockAt(duct.dx(), 0, duct.dz(), COPPER_DUCT)));
        all.addAll(extra);
        int minX = all.stream().mapToInt(BlockAt::dx).min().orElse(0);
        int maxX = all.stream().mapToInt(BlockAt::dx).max().orElse(0);
        int minY = all.stream().mapToInt(BlockAt::dy).min().orElse(0);
        int maxY = all.stream().mapToInt(BlockAt::dy).max().orElse(0);
        int minZ = all.stream().mapToInt(BlockAt::dz).min().orElse(0);
        int maxZ = all.stream().mapToInt(BlockAt::dz).max().orElse(0);

        BlockAt anchor = blockAt(all, 0, minY, 0);
        mapping.put('0', anchor == null ? ANY : mapping.get(symbols.get(anchor.block())));
        List<List<String>> layers = new ArrayList<>();
        for (int y = maxY; y >= minY; y--) {
            List<String> rows = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                StringBuilder row = new StringBuilder();
                for (int z = minZ; z <= maxZ; z++) {
                    BlockAt block = blockAt(all, x, y, z);
                    if (x == 0 && y == minY && z == 0) {
                        row.append('0');
                    } else {
                        row.append(block == null ? '_' : symbols.get(block.block()));
                    }
                }
                rows.add(row.toString());
            }
            layers.add(rows);
        }
        return new Schematic(layers, mapping);
    }

    private static BlockAt blockAt(List<BlockAt> all, int x, int y, int z) {
        for (BlockAt block : all) {
            if (block.dx() == x && block.dy() == y && block.dz() == z) {
                return block;
            }
        }
        return null;
    }
}
