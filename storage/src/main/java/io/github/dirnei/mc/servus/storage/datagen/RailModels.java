package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen for the flat rail: a centre plate plus one arm per connected horizontal side, and a flat item sprite. */
final class RailModels {

    private RailModels() {
    }

    static void rail(BlockStateProvider provider, Block block, String name) {
        ResourceLocation texture = provider.modLoc("block/" + name);
        BlockModelBuilder center = provider.models().getBuilder(name + "_center")
                .texture("particle", texture).texture("skin", texture)
                .element().from(5, 0, 5).to(11, 1, 11)
                .allFaces((direction, face) -> face.texture("#skin")).end();
        BlockModelBuilder arm = provider.models().getBuilder(name + "_arm")
                .texture("particle", texture).texture("skin", texture)
                .element().from(5, 0, 0).to(11, 1, 5)
                .allFaces((direction, face) -> face.texture("#skin")).end();

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(center).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true);

        provider.itemModels().withExistingParent(name, "item/generated").texture("layer0", texture);
    }
}
