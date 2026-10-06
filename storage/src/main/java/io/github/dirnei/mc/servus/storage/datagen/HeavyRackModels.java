package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.network.StorageNodeBlocks;
import io.github.dirnei.mc.servus.storage.rack.HeavyRackBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/**
 * Datagen for the heavy duty rack: shelves always, posts and top or bottom
 * beams only where no bay joins, a lamp part per online state, everything
 * rotated by the facing. The model space faces north: the front is z 0.
 */
final class HeavyRackModels {

    private HeavyRackModels() {
    }

    static void rack(BlockStateProvider provider, Block block, String name) {
        ResourceLocation steel = provider.modLoc("block/" + name + "_steel");
        ResourceLocation board = provider.modLoc("block/" + name + "_board");

        BlockModelBuilder shelves = model(provider, name + "_shelves", steel, board);
        box(shelves, "#board", 0, 0, 14, 16, 16, 16);   // back panel
        box(shelves, "#board", 2, 0, 2, 14, 1, 14);     // floor board
        box(shelves, "#board", 2, 8, 2, 14, 9, 14);     // middle shelf
        box(shelves, "#steel", 2, 8, 0, 14, 9, 2);      // middle front beam
        BlockModelBuilder postLeft = model(provider, name + "_post_left", steel, board);
        box(postLeft, "#steel", 0, 0, 0, 2, 16, 2);
        box(postLeft, "#steel", 0, 0, 14, 2, 16, 16);
        box(postLeft, "#steel", 0, 8, 2, 2, 9, 14);
        BlockModelBuilder postRight = model(provider, name + "_post_right", steel, board);
        box(postRight, "#steel", 14, 0, 0, 16, 16, 2);
        box(postRight, "#steel", 14, 0, 14, 16, 16, 16);
        box(postRight, "#steel", 14, 8, 2, 16, 9, 14);
        BlockModelBuilder top = model(provider, name + "_top", steel, board);
        box(top, "#steel", 0, 15, 0, 16, 16, 2);
        box(top, "#steel", 0, 15, 14, 16, 16, 16);
        BlockModelBuilder bottom = model(provider, name + "_bottom", steel, board);
        box(bottom, "#steel", 0, 0, 0, 16, 1, 2);
        box(bottom, "#steel", 0, 0, 14, 16, 1, 16);
        BlockModelBuilder lampOff = lamp(provider, name, "off");
        BlockModelBuilder lampOn = lamp(provider, name, "on");

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int angle = ((int) facing.toYRot() + 180) % 360;
            builder.part().modelFile(shelves).rotationY(angle).addModel().condition(HeavyRackBlock.FACING, facing);
            builder.part().modelFile(postLeft).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.LEFT, false);
            builder.part().modelFile(postRight).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.RIGHT, false);
            builder.part().modelFile(top).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.UP, false);
            builder.part().modelFile(bottom).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(HeavyRackBlock.DOWN, false);
            builder.part().modelFile(lampOff).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(StorageNodeBlocks.ONLINE, false);
            builder.part().modelFile(lampOn).rotationY(angle).addModel()
                    .condition(HeavyRackBlock.FACING, facing).condition(StorageNodeBlocks.ONLINE, true);
        }

        BlockModelBuilder inventory = model(provider, name + "_inventory", steel, board);
        box(inventory, "#board", 0, 0, 14, 16, 16, 16);
        box(inventory, "#board", 2, 0, 2, 14, 1, 14);
        box(inventory, "#board", 2, 8, 2, 14, 9, 14);
        box(inventory, "#steel", 2, 8, 0, 14, 9, 2);
        box(inventory, "#steel", 0, 0, 0, 2, 16, 2);
        box(inventory, "#steel", 0, 0, 14, 2, 16, 16);
        box(inventory, "#steel", 14, 0, 0, 16, 16, 2);
        box(inventory, "#steel", 14, 0, 14, 16, 16, 16);
        box(inventory, "#steel", 0, 15, 0, 16, 16, 2);
        box(inventory, "#steel", 0, 0, 0, 16, 1, 2);
        provider.simpleBlockItem(block, inventory);
    }

    private static BlockModelBuilder model(BlockStateProvider provider, String name, ResourceLocation steel, ResourceLocation board) {
        return provider.models().getBuilder(name)
                .texture("particle", steel).texture("steel", steel).texture("board", board);
    }

    private static BlockModelBuilder lamp(BlockStateProvider provider, String name, String state) {
        ResourceLocation texture = provider.modLoc("block/" + name + "_lamp_" + state);
        BlockModelBuilder builder = provider.models().getBuilder(name + "_lamp_" + state)
                .texture("particle", texture).texture("lamp", texture);
        return box(builder, "#lamp", 12, 12, 0, 14, 14, 1);
    }

    private static BlockModelBuilder box(BlockModelBuilder builder, String texture,
                                         float x1, float y1, float z1, float x2, float y2, float z2) {
        return builder.element().from(x1, y1, z1).to(x2, y2, z2)
                .allFaces((direction, face) -> face.texture(texture)).end();
    }
}
