package io.github.dirnei.mc.servus.foundation.logistics.datagen;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsBlocks;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/**
 * The Fluid Tank's connected look: a multipart block state of six glass
 * faces, each shown while its side is not joined, and twelve frame edge bars,
 * each shown while neither of its two sides is joined. That draws a frame
 * only around the outside of a formed tank.
 */
final class FluidTankModels {

    private static final int BAR = 2;

    private FluidTankModels() {
    }

    /** One frame edge: the two sides it lies between. */
    private record Edge(Direction a, Direction b) {

        String name() {
            return a.getSerializedName() + "_" + b.getSerializedName();
        }
    }

    private static List<Edge> edges() {
        List<Edge> edges = new ArrayList<>();
        Direction[] all = Direction.values();
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                if (all[i].getAxis() != all[j].getAxis()) {
                    edges.add(new Edge(all[i], all[j]));
                }
            }
        }
        return edges;
    }

    static void generate(BlockStateProvider provider) {
        ResourceLocation glass = provider.modLoc("block/fluid_tank_glass");
        ResourceLocation frame = provider.modLoc("block/fluid_tank_frame");
        MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(ServusLogisticsBlocks.FLUID_TANK.get());

        // The complete single block, for the item.
        BlockModelBuilder whole = base(provider, "block/fluid_tank", glass, frame);

        for (Direction side : Direction.values()) {
            BlockModelBuilder model = base(provider, "block/fluid_tank/glass_" + side.getSerializedName(), glass, frame);
            glassFace(model, side);
            glassFace(whole, side);
            multipart.part().modelFile(model).addModel()
                    .condition(FluidTankBlock.property(side), false).end();
        }
        for (Edge edge : edges()) {
            BlockModelBuilder model = base(provider, "block/fluid_tank/edge_" + edge.name(), glass, frame);
            edgeBar(model, edge);
            edgeBar(whole, edge);
            multipart.part().modelFile(model).addModel()
                    .condition(FluidTankBlock.property(edge.a()), false)
                    .condition(FluidTankBlock.property(edge.b()), false).end();
        }
        provider.itemModels().getBuilder(ServusLogisticsBlocks.FLUID_TANK.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile(provider.modLoc("block/fluid_tank")));
    }

    private static BlockModelBuilder base(BlockStateProvider provider, String name, ResourceLocation glass, ResourceLocation frame) {
        return provider.models().getBuilder(name)
                .parent(new ModelFile.UncheckedModelFile(provider.mcLoc("block/block")))
                .renderType("cutout")
                .texture("particle", frame)
                .texture("glass", glass)
                .texture("frame", frame);
    }

    /** A full size element with only the face toward {@code side}. */
    private static void glassFace(BlockModelBuilder model, Direction side) {
        model.element().from(0, 0, 0).to(16, 16, 16)
                .face(side).texture("#glass").cullface(side).end()
                .end();
    }

    /** A 2 px bar along the edge between the two sides, running the full length of the third axis. */
    private static void edgeBar(BlockModelBuilder model, Edge edge) {
        float[] from = {0, 0, 0};
        float[] to = {16, 16, 16};
        for (Direction side : new Direction[] {edge.a(), edge.b()}) {
            int axis = side.getAxis().ordinal();
            if (side.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                from[axis] = 16 - BAR;
            } else {
                to[axis] = BAR;
            }
        }
        model.element().from(from[0], from[1], from[2]).to(to[0], to[1], to[2])
                .allFaces((direction, face) -> face.texture("#frame"))
                .end();
    }
}
