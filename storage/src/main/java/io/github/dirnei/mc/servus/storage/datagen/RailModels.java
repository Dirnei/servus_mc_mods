package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.lib.block.ConnectableBlock;
import io.github.dirnei.mc.servus.storage.rail.RailBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/** Datagen for the flat rail: a centre plate shaped by the connections (straight, curve or cross), one arm per connected side, a dock piece per side that touches a storage block, and a flat item sprite. */
final class RailModels {

    private RailModels() {
    }

    static void rail(BlockStateProvider provider, Block block, String name) {
        ResourceLocation texture = provider.modLoc("block/" + name);
        BlockModelBuilder center = center(provider, name + "_center", texture);
        BlockModelBuilder curve = center(provider, name + "_center_curve", provider.modLoc("block/" + name + "_curve"));
        BlockModelBuilder cross = center(provider, name + "_center_cross", provider.modLoc("block/" + name + "_cross"));
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
        for (int mask = 0; mask < 16; mask++) {
            centerPart(builder, mask, center, curve, cross);
        }
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

    private static BlockModelBuilder center(BlockStateProvider provider, String model, ResourceLocation texture) {
        return provider.models().getBuilder(model)
                .texture("particle", texture).texture("skin", texture)
                .element().from(5, 0, 5).to(11, 1, 11)
                .allFaces((direction, face) -> face.texture("#skin")).end();
    }

    /** One centre part for a rail mask (bits north, east, south, west), conditioned on all four sides so exactly one applies. */
    private static void centerPart(MultiPartBlockStateBuilder builder, int mask, BlockModelBuilder center,
                                   BlockModelBuilder curve, BlockModelBuilder cross) {
        boolean north = (mask & 1) != 0;
        boolean east = (mask & 2) != 0;
        boolean south = (mask & 4) != 0;
        boolean west = (mask & 8) != 0;
        BlockModelBuilder model;
        int rotation;
        if (Integer.bitCount(mask) >= 3) {
            model = cross;
            rotation = 0;
        } else if (Integer.bitCount(mask) == 2 && north != south) {
            model = curve;
            rotation = north ? (east ? 0 : 270) : (east ? 90 : 180);
        } else {
            model = center;
            rotation = (east || west) ? 90 : 0;
        }
        MultiPartBlockStateBuilder.PartBuilder part = builder.part().modelFile(model).rotationY(rotation).addModel();
        side(part, ConnectableBlock.NORTH, RailBlock.DOCK_NORTH, north);
        side(part, ConnectableBlock.EAST, RailBlock.DOCK_EAST, east);
        side(part, ConnectableBlock.SOUTH, RailBlock.DOCK_SOUTH, south);
        side(part, ConnectableBlock.WEST, RailBlock.DOCK_WEST, west);
    }

    /** A side runs rail into the centre when it connects without a dock; a dock is drawn on its own and leaves the centre alone. */
    private static void side(MultiPartBlockStateBuilder.PartBuilder part, BooleanProperty connected, BooleanProperty dock, boolean rail) {
        if (rail) {
            part.nestedGroup().condition(connected, true).condition(dock, false).end();
        } else {
            part.nestedGroup().useOr().condition(connected, false).condition(dock, true).end();
        }
    }
}
