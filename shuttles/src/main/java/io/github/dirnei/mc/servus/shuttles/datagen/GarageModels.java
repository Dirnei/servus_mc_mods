package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.material.MaterialKind;
import io.github.dirnei.mc.servus.shuttles.garage.GarageDoorLook;
import io.github.dirnei.mc.servus.shuttles.rail.RailLook;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;

/**
 * The garage model, front to the north and doorway to the south: wood board
 * walls, roof and floor around a cell with the rail running in through a
 * {@link GarageDoorLook 12 by 12 pixel} opening in the back, and the frame
 * textures on thin planes {@link #FRAME_OUT} outside every outer face, the
 * back one cut round the opening.
 */
final class GarageModels {

    /** How far the frame planes stand outside the board faces, in pixels. */
    private static final float FRAME_OUT = 0.02F;
    /** How far the rail inside keeps off the walls it would touch, in pixels. */
    private static final float RAIL_INSET = 0.05F;

    private GarageModels() {
    }

    /** One garage model with that front texture over the shared side, top and back. */
    static ModelFile garage(BlockStateProvider provider, String model, String front) {
        BlockModelBuilder builder = provider.models().withExistingParent(model, "minecraft:block/block")
                .texture("particle", provider.modLoc("block/" + front))
                .texture("board", MaterialKind.WOOD.placeholder())
                .texture("north", provider.modLoc("block/" + front))
                .texture("south", provider.modLoc("block/shuttle_garage_back"))
                .texture("side", provider.modLoc("block/shuttle_garage_side"))
                .texture("top", provider.modLoc("block/shuttle_garage_top"))
                .texture("head", provider.modLoc("block/rail_head"));
        walls(builder);
        frame(builder);
        rail(builder);
        return builder;
    }

    private static void walls(BlockModelBuilder builder) {
        float x0 = GarageDoorLook.DOOR_X0;
        float x1 = GarageDoorLook.DOOR_X1;
        float top = GarageDoorLook.DOOR_TOP;
        // Front wall, side walls, roof, lintel and the posts beside the opening.
        board(builder, 0, 0, 0, 16, 16, 1, EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST));
        board(builder, 0, 0, 1, 1, 16, 16, EnumSet.of(Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN, Direction.SOUTH));
        board(builder, 15, 0, 1, 16, 16, 16, EnumSet.of(Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN, Direction.SOUTH));
        board(builder, 1, top, 1, 15, 16, 15, EnumSet.of(Direction.UP, Direction.DOWN));
        board(builder, 1, top, 15, 15, 16, 16, EnumSet.of(Direction.SOUTH, Direction.UP, Direction.DOWN));
        board(builder, 1, 0, 15, x0, top, 16, EnumSet.of(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.DOWN));
        board(builder, x1, 0, 15, 15, top, 16, EnumSet.of(Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.DOWN));
        // The floor seen from below, left open where the posts stand.
        board(builder, 1, 0, 1, 15, 0, 15, EnumSet.of(Direction.DOWN));
        board(builder, x0, 0, 15, x1, 0, 16, EnumSet.of(Direction.DOWN));
    }

    /** A board box with only the given faces; faces on the block's outside are culled against a full neighbour. */
    private static void board(BlockModelBuilder builder, float x0, float y0, float z0, float x1, float y1, float z1, Set<Direction> faces) {
        ModelBuilder<BlockModelBuilder>.ElementBuilder element = builder.element().from(x0, y0, z0).to(x1, y1, z1);
        for (Direction side : faces) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder.FaceBuilder face = element.face(side).texture("#board");
            if (onOutside(side, x0, y0, z0, x1, y1, z1)) {
                face.cullface(side);
            }
            face.end();
        }
        element.end();
    }

    private static boolean onOutside(Direction side, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (side) {
            case DOWN -> y0 == 0;
            case UP -> y1 == 16;
            case NORTH -> z0 == 0;
            case SOUTH -> z1 == 16;
            case WEST -> x0 == 0;
            case EAST -> x1 == 16;
        };
    }

    /** The frame textures on planes just outside the board, the back plane in three pieces round the opening. */
    private static void frame(BlockModelBuilder builder) {
        float out = FRAME_OUT;
        plane(builder, Direction.NORTH, "#north", -out, -out, -out, 16 + out, 16 + out, -out);
        plane(builder, Direction.WEST, "#side", -out, -out, -out, -out, 16 + out, 16 + out);
        plane(builder, Direction.EAST, "#side", 16 + out, -out, -out, 16 + out, 16 + out, 16 + out);
        plane(builder, Direction.UP, "#top", -out, 16 + out, -out, 16 + out, 16 + out, 16 + out);
        plane(builder, Direction.DOWN, "#top", -out, -out, -out, 16 + out, -out, 16 + out);
        float x0 = GarageDoorLook.DOOR_X0;
        float x1 = GarageDoorLook.DOOR_X1;
        float top = GarageDoorLook.DOOR_TOP;
        plane(builder, Direction.SOUTH, "#south", -out, top, 16 + out, 16 + out, 16 + out, 16 + out);
        plane(builder, Direction.SOUTH, "#south", -out, -out, 16 + out, x0, top, 16 + out);
        plane(builder, Direction.SOUTH, "#south", x1, -out, 16 + out, 16 + out, top, 16 + out);
    }

    /**
     * A flat element with one face, its texture mapped by block position as
     * a full cube face would be, so the pieces of one side line up.
     */
    private static void plane(BlockModelBuilder builder, Direction side, String texture, float x0, float y0, float z0, float x1, float y1,
                              float z1) {
        float[] uv = switch (side) {
            case NORTH -> new float[] {16 - x1, 16 - y1, 16 - x0, 16 - y0};
            case SOUTH -> new float[] {x0, 16 - y1, x1, 16 - y0};
            case WEST -> new float[] {z0, 16 - y1, z1, 16 - y0};
            case EAST -> new float[] {16 - z1, 16 - y1, 16 - z0, 16 - y0};
            case UP -> new float[] {x0, z0, x1, z1};
            case DOWN -> new float[] {x0, 16 - z1, x1, 16 - z0};
        };
        for (int i = 0; i < uv.length; i++) {
            uv[i] = Math.clamp(uv[i], 0, 16);
        }
        builder.element().from(x0, y0, z0).to(x1, y1, z1)
                .face(side).texture(texture).uvs(uv[0], uv[1], uv[2], uv[3]).cullface(side).end()
                .end();
    }

    /**
     * The track from just inside the front wall out through the opening:
     * both heads and the sleepers of a straight rail, kept off the walls and
     * without bottoms, which the floor covers.
     */
    private static void rail(BlockModelBuilder builder) {
        for (RailLook.Box box : RailLook.boxes(RailLook.Piece.STRAIGHT)) {
            float x0 = (float) box.x0();
            float x1 = (float) box.x1();
            float z0 = (float) Math.max(box.z0(), 1 + RAIL_INSET);
            float z1 = (float) box.z1();
            boolean sleeper = box.part() == RailLook.Part.SLEEPER;
            if (sleeper) {
                x0 = Math.max(x0, 1 + RAIL_INSET);
                x1 = Math.min(x1, 15 - RAIL_INSET);
                z1 = Math.min(z1, 15 - RAIL_INSET);
            }
            ModelBuilder<BlockModelBuilder>.ElementBuilder element = builder.element()
                    .from(x0, (float) box.y0(), z0).to(x1, (float) box.y1(), z1);
            String texture = sleeper ? "#board" : "#head";
            element.allFacesExcept((side, face) -> face.texture(texture), Set.of(Direction.DOWN));
            element.end();
        }
    }
}
