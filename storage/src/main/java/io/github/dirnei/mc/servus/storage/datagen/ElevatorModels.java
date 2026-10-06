package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen for the elevator segment: four corner posts, the rail centre, and the rail arm per connected horizontal side. */
final class ElevatorModels {

    private ElevatorModels() {
    }

    static void elevator(BlockStateProvider provider, Block block, String name, String railName) {
        ResourceLocation texture = provider.modLoc("block/" + name);
        BlockModelBuilder frame = provider.models().getBuilder(name + "_frame")
                .texture("particle", texture).texture("skin", texture);
        int[][] posts = {{0, 0}, {14, 0}, {0, 14}, {14, 14}};
        for (int[] post : posts) {
            frame.element().from(post[0], 0, post[1]).to(post[0] + 2, 16, post[1] + 2)
                    .allFaces((direction, face) -> face.texture("#skin")).end();
        }
        ModelFile center = provider.models().getExistingFile(provider.modLoc("block/" + railName + "_center"));
        ModelFile arm = provider.models().getExistingFile(provider.modLoc("block/" + railName + "_arm"));

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(frame).addModel();
        builder.part().modelFile(center).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true);

        provider.simpleBlockItem(block, frame);
    }
}
