package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen for the flat rail: a centre plate, one arm per connected side, a dock piece per side that touches a storage block, and a flat item sprite. */
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

        ResourceLocation dockTexture = provider.modLoc("block/" + name + "_dock");
        BlockModelBuilder dock = provider.models().getBuilder(name + "_dock")
                .texture("particle", texture).texture("skin", texture).texture("dock", dockTexture)
                .element().from(5, 0, 1).to(7, 1, 5)
                .allFaces((direction, face) -> face.texture("#dock")).end()
                .element().from(9, 0, 1).to(11, 1, 5)
                .allFaces((direction, face) -> face.texture("#dock")).end()
                .element().from(4, 0, 0).to(12, 3, 1)
                .allFaces((direction, face) -> face.texture("#dock")).end();

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(center).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true).condition(RailBlock.DOCK_NORTH, false);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true).condition(RailBlock.DOCK_EAST, false);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true).condition(RailBlock.DOCK_SOUTH, false);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true).condition(RailBlock.DOCK_WEST, false);
        builder.part().modelFile(dock).addModel().condition(RailBlock.DOCK_NORTH, true);
        builder.part().modelFile(dock).rotationY(90).addModel().condition(RailBlock.DOCK_EAST, true);
        builder.part().modelFile(dock).rotationY(180).addModel().condition(RailBlock.DOCK_SOUTH, true);
        builder.part().modelFile(dock).rotationY(270).addModel().condition(RailBlock.DOCK_WEST, true);

        provider.itemModels().withExistingParent(name, "item/generated").texture("layer0", texture);
    }
}
