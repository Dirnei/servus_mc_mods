package io.github.dirnei.mc.servus.shuttles.cpu;

import io.github.dirnei.mc.servus.foundation.graph.Side;

/**
 * Which tile of a formed CPU face a block shows, from the block's place
 * class in the box (see {@link CpuShape}): a block whose class is low or
 * high on an axis sits on that side of the box. A tile is named by the
 * edges of the box it touches, as seen on the texture: {@code tile_tl} has
 * frame on its top and left, {@code tile_c} has none. Texture sides follow
 * vanilla's default face UVs: on the north face the texture's left is east,
 * on the south face west, on the west face north, on the east face south;
 * on the up face the top is north and the left west, on the down face the
 * top is south and the left west.
 */
public final class CpuTiles {

    private CpuTiles() {
    }

    /** The tile of a part's face; faces inside the box get the centre tile. */
    public static String tile(int part, Side face) {
        CpuShape.Vec at = CpuShape.offset(part);
        if (!onBoundary(at, face)) {
            return "tile_c";
        }
        Side[] sides = textureSides(face);
        boolean top = onBoundary(at, sides[2]);
        boolean bottom = onBoundary(at, sides[3]);
        boolean left = onBoundary(at, sides[0]);
        boolean right = onBoundary(at, sides[1]);
        StringBuilder name = new StringBuilder("tile_");
        if (top) {
            name.append('t');
        } else if (bottom) {
            name.append('b');
        }
        if (left) {
            name.append('l');
        } else if (right) {
            name.append('r');
        }
        return name.length() == "tile_".length() ? "tile_c" : name.toString();
    }

    /** The world sides that are the texture's left, right, top and bottom on a face. */
    static Side[] textureSides(Side face) {
        return switch (face) {
            case NORTH -> new Side[] {Side.EAST, Side.WEST, Side.UP, Side.DOWN};
            case SOUTH -> new Side[] {Side.WEST, Side.EAST, Side.UP, Side.DOWN};
            case WEST -> new Side[] {Side.NORTH, Side.SOUTH, Side.UP, Side.DOWN};
            case EAST -> new Side[] {Side.SOUTH, Side.NORTH, Side.UP, Side.DOWN};
            case UP -> new Side[] {Side.WEST, Side.EAST, Side.NORTH, Side.SOUTH};
            case DOWN -> new Side[] {Side.WEST, Side.EAST, Side.SOUTH, Side.NORTH};
        };
    }

    /** Whether the block sits on the box's outer layer toward a side. */
    static boolean onBoundary(CpuShape.Vec at, Side side) {
        int coordinate = side.dx() != 0 ? at.x() : side.dy() != 0 ? at.y() : at.z();
        int step = side.dx() + side.dy() + side.dz();
        return step > 0 ? coordinate == CpuShape.SIZE - 1 : coordinate == 0;
    }
}
