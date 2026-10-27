package io.github.dirnei.mc.servus.shuttles.datagen;

import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.BACK_POST_Z;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.BOARD_Z;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLOOR_TOP;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.GLASS_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.GLASS_Z0;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.GLASS_Z1;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.PORT_DEPTH;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.PORT_HALF;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.PORT_HALF_WIDTH;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.POST;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.TOP_BOTTOM;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Datagen for the Fluid Rack, built facing north: front at z 0, the board
 * back at z 15 to 16. Parts: the core (back and floor boards, front floor
 * beam, top plate, glass front) always; at a side with no joined bay a front
 * and a back post with side beams, a ladder rung and a side glass pane;
 * where a bay joins, the floor, top and glass run through to the block edge
 * instead; the copper port flange on the port bay. Frame parts meet only
 * at their edges; glass panes have only their two broad faces and stand
 * 0.05 off every frame part; the flange passes through the glass, so no two
 * faces share a plane where they overlap.
 */
final class FluidRackModels {

    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("servus_foundation", "block/fluid_tank_glass");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("block/copper_block");
    private static final Set<Direction> FRONT_AND_BACK = EnumSet.of(Direction.NORTH, Direction.SOUTH);
    private static final Set<Direction> SIDES = EnumSet.of(Direction.EAST, Direction.WEST);
    private static final Set<Direction> ALL = EnumSet.allOf(Direction.class);

    private FluidRackModels() {
    }

    static void rack(BlockStateProvider provider, Block block, String name) {
        Identifier iron = provider.modLoc("block/heavy_rack_iron");
        Identifier board = provider.modLoc("block/heavy_rack_board");

        BlockModelBuilder core = model(provider, name + "_core", iron, board);
        core(core);
        BlockModelBuilder postLeft = model(provider, name + "_post_left", iron, board);
        post(postLeft, 0);
        BlockModelBuilder postRight = model(provider, name + "_post_right", iron, board);
        post(postRight, 16 - POST);
        BlockModelBuilder extLeft = model(provider, name + "_ext_left", iron, board);
        extension(extLeft, 0);
        BlockModelBuilder extRight = model(provider, name + "_ext_right", iron, board);
        extension(extRight, 16 - POST);
        BlockModelBuilder port = model(provider, name + "_port", iron, board);
        port(port);

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            builder.part().modelFile(core).rotationY(angle).addModel().condition(FluidRackBlock.FACING, facing);
            builder.part().modelFile(postLeft).rotationY(angle).addModel()
                    .condition(FluidRackBlock.FACING, facing).condition(FluidRackBlock.LEFT, false);
            builder.part().modelFile(extLeft).rotationY(angle).addModel()
                    .condition(FluidRackBlock.FACING, facing).condition(FluidRackBlock.LEFT, true);
            builder.part().modelFile(postRight).rotationY(angle).addModel()
                    .condition(FluidRackBlock.FACING, facing).condition(FluidRackBlock.RIGHT, false);
            builder.part().modelFile(extRight).rotationY(angle).addModel()
                    .condition(FluidRackBlock.FACING, facing).condition(FluidRackBlock.RIGHT, true);
            builder.part().modelFile(port).rotationY(angle).addModel()
                    .condition(FluidRackBlock.FACING, facing).condition(FluidRackBlock.PORT, true);
        }

        // The block parent gives the item the turned block view; without it the GUI shows the back board face on.
        BlockModelBuilder inventory = provider.models().withExistingParent(name + "_inventory", "minecraft:block/block")
                .texture("particle", iron).texture("iron", iron).texture("board", board).texture("glass", GLASS).texture("copper", COPPER);
        core(inventory);
        post(inventory, 0);
        post(inventory, 16 - POST);
        port(inventory);
        provider.simpleBlockItem(block, inventory);
        provider.itemModels().itemModelType(provider.modLoc(name), ServusShuttlesIds.MOD_ID + ":rack_wood");
    }

    /** Back and floor boards, the front floor beam, the top plate and the glass front between the posts. */
    private static void core(BlockModelBuilder builder) {
        box(builder, "#board", 0, 0, BOARD_Z, 16, 16, 16, ALL);
        box(builder, "#board", POST, 0, POST, 16 - POST, FLOOR_TOP, BOARD_Z, ALL);
        box(builder, "#iron", POST, 0, 0, 16 - POST, FLOOR_TOP, POST, ALL);
        box(builder, "#iron", POST, TOP_BOTTOM, 0, 16 - POST, 16, BOARD_Z, ALL);
        box(builder, "#glass", POST + GLASS_INSET, FLOOR_TOP + GLASS_INSET, GLASS_Z0, 16 - POST - GLASS_INSET, TOP_BOTTOM - GLASS_INSET,
                GLASS_Z1, FRONT_AND_BACK);
    }

    /** A front and a back post, the side beams top and bottom, a ladder rung and the side glass at the x edge {@code x}. */
    private static void post(BlockModelBuilder builder, float x) {
        box(builder, "#iron", x, 0, 0, x + POST, 16, POST, ALL);
        box(builder, "#iron", x, 0, BACK_POST_Z, x + POST, 16, BOARD_Z, ALL);
        box(builder, "#iron", x, TOP_BOTTOM, POST, x + POST, 16, BACK_POST_Z, ALL);
        box(builder, "#iron", x, 0, POST, x + POST, FLOOR_TOP, BACK_POST_Z, ALL);
        box(builder, "#iron", x, 8, POST, x + POST, 9, BACK_POST_Z, ALL);
        float pane = x + POST / 2;
        box(builder, "#glass", pane - 0.1F, FLOOR_TOP + GLASS_INSET, POST + GLASS_INSET, pane + 0.1F, TOP_BOTTOM - GLASS_INSET,
                BACK_POST_Z - GLASS_INSET, SIDES);
    }

    /** The floor beam and board, the top plate and the glass front running through to the block edge where a bay joins. */
    private static void extension(BlockModelBuilder builder, float x) {
        box(builder, "#iron", x, 0, 0, x + POST, FLOOR_TOP, POST, ALL);
        box(builder, "#board", x, 0, POST, x + POST, FLOOR_TOP, BOARD_Z, ALL);
        box(builder, "#iron", x, TOP_BOTTOM, 0, x + POST, 16, BOARD_Z, ALL);
        float x0 = x == 0 ? 0 : x + GLASS_INSET;
        float x1 = x == 0 ? POST + GLASS_INSET : 16;
        box(builder, "#glass", x0, FLOOR_TOP + GLASS_INSET, GLASS_Z0, x1, TOP_BOTTOM - GLASS_INSET, GLASS_Z1, FRONT_AND_BACK);
    }

    /** The copper flange of the port at a tanker's coupling height, through the glass into the bay. */
    private static void port(BlockModelBuilder builder) {
        float y = FluidRackLook.portY();
        box(builder, "#copper", 8 - PORT_HALF_WIDTH, y - PORT_HALF, 0, 8 + PORT_HALF_WIDTH, y + PORT_HALF, PORT_DEPTH, ALL);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, Identifier iron, Identifier board) {
        return provider.models().getBuilder(name)
                .texture("particle", iron).texture("iron", iron).texture("board", board).texture("glass", GLASS).texture("copper", COPPER);
    }

    private static void box(BlockModelBuilder builder, String texture, float x1, float y1, float z1, float x2, float y2, float z2,
                            Set<Direction> faces) {
        var element = builder.element().from(x1, y1, z1).to(x2, y2, z2);
        for (Direction face : faces) {
            element.face(face).texture(texture);
        }
        element.end();
    }
}
