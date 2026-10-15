package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.rack.HeavyRackBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Datagen for the heavy duty rack. Every box is disjoint from every other so
 * no faces fight. The model space faces north: front at z 0, back panel at
 * z 15 to 16 behind the posts, which sit at z 0 to 2 and 13 to 15.
 * Parts: core always; posts where no bay joins; extensions where one does;
 * top and bottom beams always, so stacked bays show where they meet.
 */
final class HeavyRackModels {

    private HeavyRackModels() {
    }

    static void rack(BlockStateProvider provider, Block block, String name) {
        Identifier iron = provider.modLoc("block/" + name + "_iron");
        Identifier board = provider.modLoc("block/" + name + "_board");

        BlockModelBuilder core = model(provider, name + "_core", iron, board);
        core(core);
        BlockModelBuilder postLeft = model(provider, name + "_post_left", iron, board);
        post(postLeft, 0);
        BlockModelBuilder postRight = model(provider, name + "_post_right", iron, board);
        post(postRight, 14);
        BlockModelBuilder extLeft = model(provider, name + "_ext_left", iron, board);
        extension(extLeft, 0);
        BlockModelBuilder extRight = model(provider, name + "_ext_right", iron, board);
        extension(extRight, 14);
        BlockModelBuilder top = model(provider, name + "_top", iron, board);
        beams(top, 15, 2, 14);
        BlockModelBuilder bottom = model(provider, name + "_bottom", iron, board);
        beams(bottom, 0, 2, 14);
        BlockModelBuilder topLeft = model(provider, name + "_top_left", iron, board);
        beams(topLeft, 15, 0, 2);
        BlockModelBuilder topRight = model(provider, name + "_top_right", iron, board);
        beams(topRight, 15, 14, 16);
        BlockModelBuilder bottomLeft = model(provider, name + "_bottom_left", iron, board);
        beams(bottomLeft, 0, 0, 2);
        BlockModelBuilder bottomRight = model(provider, name + "_bottom_right", iron, board);
        beams(bottomRight, 0, 14, 16);

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ModelRotations.rotationY(facing);
            builder.part().modelFile(core).rotationY(angle).addModel().condition(HeavyRackBlock.FACING, facing);
            builder.part().modelFile(postLeft).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.LEFT, false);
            builder.part().modelFile(extLeft).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.LEFT, true);
            builder.part().modelFile(postRight).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.RIGHT, false);
            builder.part().modelFile(extRight).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.RIGHT, true);
            builder.part().modelFile(top).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing);
            builder.part().modelFile(topLeft).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.LEFT, true);
            builder.part().modelFile(topRight).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.RIGHT, true);
            builder.part().modelFile(bottom).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing);
            builder.part().modelFile(bottomLeft).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.LEFT, true);
            builder.part().modelFile(bottomRight).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.RIGHT, true);
        }

        BlockModelBuilder inventory = model(provider, name + "_inventory", iron, board);
        core(inventory);
        post(inventory, 0);
        post(inventory, 14);
        beams(inventory, 15, 2, 14);
        beams(inventory, 0, 2, 14);
        provider.simpleBlockItem(block, inventory);
        provider.itemModels().itemModelType(provider.modLoc(name), ServusShuttlesIds.MOD_ID + ":rack_wood");
    }

    /** Back panel, floor board, middle shelf and the front mid beam between the posts. */
    private static void core(BlockModelBuilder builder) {
        box(builder, "#board", 0, 0, 15, 16, 16, 16);
        box(builder, "#board", 2, 0, 2, 14, 1, 13);
        box(builder, "#board", 2, 8, 2, 14, 9, 13);
        box(builder, "#iron", 2, 8, 0, 14, 9, 2);
    }

    /** Two posts and the side beam at the given x edge, 0 for left, 14 for right. */
    private static void post(BlockModelBuilder builder, int x) {
        box(builder, "#iron", x, 0, 0, x + 2, 16, 2);
        box(builder, "#iron", x, 0, 13, x + 2, 16, 15);
        box(builder, "#iron", x, 8, 2, x + 2, 9, 13);
    }

    /** Floor, shelf and front mid beam running through to the bay edge where no post stands. */
    private static void extension(BlockModelBuilder builder, int x) {
        box(builder, "#board", x, 0, 2, x + 2, 1, 13);
        box(builder, "#board", x, 8, 2, x + 2, 9, 13);
        box(builder, "#iron", x, 8, 0, x + 2, 9, 2);
    }

    /** Front and back beams at the given height across x1 to x2. */
    private static void beams(BlockModelBuilder builder, int y, int x1, int x2) {
        box(builder, "#iron", x1, y, 0, x2, y + 1, 2);
        box(builder, "#iron", x1, y, 13, x2, y + 1, 15);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, Identifier iron, Identifier board) {
        return provider.models().getBuilder(name)
                .texture("particle", iron).texture("iron", iron).texture("board", board);
    }

    private static void box(BlockModelBuilder builder, String texture,
                            float x1, float y1, float z1, float x2, float y2, float z2) {
        builder.element().from(x1, y1, z1).to(x2, y2, z2)
                .allFaces((direction, face) -> face.texture(texture)).end();
    }
}
