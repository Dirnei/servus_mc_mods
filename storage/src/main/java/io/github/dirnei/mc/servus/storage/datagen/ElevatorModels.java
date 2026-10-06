package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen for the elevator segment: a full height pillar plus the rail arm per connected horizontal side. */
final class ElevatorModels {

    private ElevatorModels() {
    }

    static void elevator(BlockStateProvider provider, Block block, String name, String railName) {
        ResourceLocation texture = provider.modLoc("block/" + name);
        BlockModelBuilder pillar = provider.models().getBuilder(name + "_center")
                .texture("particle", texture).texture("skin", texture)
                .element().from(4, 0, 4).to(12, 16, 12)
                .allFaces((direction, face) -> face.texture("#skin")).end();
        ModelFile arm = provider.models().getExistingFile(provider.modLoc("block/" + railName + "_arm"));

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(pillar).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true);

        provider.simpleBlockItem(block, pillar);
    }
}
