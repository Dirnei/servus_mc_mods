package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen helper for {@link ConnectableBlock}: a centre cube plus one arm per connected side. */
public final class ConnectableModels {

    private ConnectableModels() {
    }

    /**
     * @param radius   half width of the core in pixels, matching the block
     * @param withItem whether to also write an inventory model for the block item
     */
    public static void connectable(BlockStateProvider provider, Block block, String name, int radius, boolean withItem) {
        int lo = 8 - radius;
        int hi = 8 + radius;
        ResourceLocation texture = provider.modLoc("block/" + name);
        BlockModelBuilder center = provider.models().getBuilder(name + "_center")
                .texture("particle", texture).texture("skin", texture)
                .element().from(lo, lo, lo).to(hi, hi, hi)
                .allFaces((direction, face) -> face.texture("#skin")).end();
        BlockModelBuilder arm = provider.models().getBuilder(name + "_arm")
                .texture("particle", texture).texture("skin", texture)
                .element().from(lo, lo, 0).to(hi, hi, lo)
                .allFaces((direction, face) -> face.texture("#skin")).end();

        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        builder.part().modelFile(center).addModel();
        builder.part().modelFile(arm).addModel().condition(ConnectableBlock.NORTH, true);
        builder.part().modelFile(arm).rotationY(90).addModel().condition(ConnectableBlock.EAST, true);
        builder.part().modelFile(arm).rotationY(180).addModel().condition(ConnectableBlock.SOUTH, true);
        builder.part().modelFile(arm).rotationY(270).addModel().condition(ConnectableBlock.WEST, true);
        builder.part().modelFile(arm).rotationX(270).addModel().condition(ConnectableBlock.UP, true);
        builder.part().modelFile(arm).rotationX(90).addModel().condition(ConnectableBlock.DOWN, true);

        if (withItem) {
            BlockModelBuilder inventory = provider.models().getBuilder(name + "_inventory")
                    .texture("particle", texture).texture("skin", texture)
                    .element().from(lo, lo, 0).to(hi, hi, 16)
                    .allFaces((direction, face) -> face.texture("#skin")).end();
            provider.simpleBlockItem(block, inventory);
        }
    }
}
