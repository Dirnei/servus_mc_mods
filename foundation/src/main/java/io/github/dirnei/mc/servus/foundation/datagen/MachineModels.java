package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Model helpers shared by Foundation's machine models. */
final class MachineModels {

    private MachineModels() {
    }

    /** A model under {@code models/block/} with {@code minecraft:block/block} as parent. */
    static BlockModelBuilder blockModel(BlockStateProvider provider, String name) {
        return provider.models().withExistingParent(name, provider.mcLoc("block/block"));
    }

    /**
     * A box with the UVs vanilla would pick for it at its own height, moved up
     * by {@code dy}: the side texture on the sides, {@code ends} on top and bottom.
     *
     * @param b {@code {x0, y0, z0, x1, y1, z1}} in pixels
     */
    static BlockModelBuilder.ElementBuilder box(BlockModelBuilder model, float[] b, float dy, String sides, String ends) {
        return box(model, b, dy, sides, ends, Set.of());
    }

    /**
     * {@link #box(BlockModelBuilder, float[], float, String, String)} without the faces in {@code skip}, for parts
     * that rest on another part. A box reaching past the block's edge gets its UVs shifted back into the texture.
     */
    static BlockModelBuilder.ElementBuilder box(BlockModelBuilder model, float[] b, float dy, String sides, String ends, Set<Direction> skip) {
        BlockModelBuilder.ElementBuilder element = model.element().from(b[0], b[1] + dy, b[2]).to(b[3], b[4] + dy, b[5]);
        float[] x = fitted(b[0], b[3]);
        float[] y = fitted(b[1], b[4]);
        float[] z = fitted(b[2], b[5]);
        Map<Direction, float[]> uvs = Map.of(
                Direction.DOWN, new float[] {x[0], 16 - z[1], x[1], 16 - z[0]},
                Direction.UP, new float[] {x[0], z[0], x[1], z[1]},
                Direction.NORTH, new float[] {16 - x[1], 16 - y[1], 16 - x[0], 16 - y[0]},
                Direction.SOUTH, new float[] {x[0], 16 - y[1], x[1], 16 - y[0]},
                Direction.WEST, new float[] {z[0], 16 - y[1], z[1], 16 - y[0]},
                Direction.EAST, new float[] {16 - z[1], 16 - y[1], 16 - z[0], 16 - y[0]});
        for (Direction direction : FACE_ORDER) {
            if (!skip.contains(direction)) {
                float[] uv = uvs.get(direction);
                String texture = direction.getAxis() == Direction.Axis.Y ? ends : sides;
                element.face(direction).texture(texture).uvs(uv[0], uv[1], uv[2], uv[3]).end();
            }
        }
        return element;
    }

    /** The face order models have always been written in. */
    private static final List<Direction> FACE_ORDER = List.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST);

    /** A span of at most 16 pixels moved into 0..16, so its UVs stay on the texture. */
    private static float[] fitted(float from, float to) {
        float shift = from < 0 ? -from : to > 16 ? 16 - to : 0;
        return new float[] {from + shift, to + shift};
    }

    /** A thin rod moved up by {@code dy}; its sides take a one pixel strip of {@code texture}, since it can be taller than a block. */
    static void rod(BlockModelBuilder model, float[] rod, float dy, String texture) {
        model.element().from(rod[0], rod[1] + dy, rod[2]).to(rod[3], rod[4] + dy, rod[5])
                .allFaces((direction, face) -> face.texture(texture).uvs(7, 0, 8, 16))
                .face(Direction.UP).uvs(7, 7, 8, 8).end()
                .face(Direction.DOWN).uvs(7, 7, 8, 8).end();
    }

    /** Display transforms for an item model two blocks tall, scaled down so it fits a slot. */
    static void twoBlockDisplay(BlockModelBuilder model) {
        display(model, ItemDisplayContext.GUI, 30, 225, 0.4F);
        display(model, ItemDisplayContext.GROUND, 0, 0, 0.2F);
        display(model, ItemDisplayContext.FIXED, 0, 0, 0.4F);
        display(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 0, 225, 0.3F);
    }

    /** Lowers the model by half its 32 pixel height after scaling. */
    private static void display(BlockModelBuilder model, ItemDisplayContext context, float rotX, float rotY, float scale) {
        model.transforms().transform(context).rotation(rotX, rotY, 0).translation(0, -8 * scale, 0).scale(scale).end();
    }

    /** Multipart parts for the pipe stubs of a pump's lower half, one per side and stub size. */
    static void pumpStubParts(BlockStateProvider provider, MultiPartBlockStateBuilder builder) {
        for (Map.Entry<Direction, IntegerProperty> stub : PumpBlock.STUBS.entrySet()) {
            for (int size = 1; size <= PumpBlock.MAX_STUB; size++) {
                builder.part().modelFile(pumpStub(provider, stub.getKey(), size)).addModel()
                        .condition(stub.getValue(), size).condition(BeamPumpBlock.HALF, DoubleBlockHalf.LOWER).end();
            }
        }
    }

    /** A short pipe from the pump body to the block edge, as thick as a duct arm with {@code size} lines. */
    private static ModelFile pumpStub(BlockStateProvider provider, Direction direction, int size) {
        BlockModelBuilder model = blockModel(provider, "pump_stub_" + direction.getSerializedName() + "_" + size)
                .texture("particle", provider.modLoc("block/machine_side"))
                .texture("side", provider.modLoc("block/machine_side"))
                .texture("top", provider.modLoc("block/machine_top"));
        int[] box = PumpBlock.stubBox(direction, size);
        int lo = 8 - PumpBlock.stubRadius(size);
        int hi = 8 + PumpBlock.stubRadius(size);
        model.element().from(box[0], box[1], box[2]).to(box[3], box[4], box[5])
                .allFaces((face, builder) -> builder.texture(face == direction || face == direction.getOpposite() ? "#top" : "#side")
                        .uvs(lo, lo, hi, hi))
                .face(direction).cullface(direction).end();
        return model;
    }

    /**
     * The Ender Anchor: obsidian front with the shut eye in its socket (the
     * renderer draws the open eye over it), chunk grid sides, and a top whose
     * purple core brightens while it holds its chunk.
     */
    static void enderAnchor(BlockStateProvider provider) {
        Block block = MachineContent.ENDER_ANCHOR.get();
        Identifier side = provider.modLoc("block/ender_anchor_side");
        Identifier front = provider.modLoc("block/ender_anchor_front");
        ModelFile unlit = provider.models().orientable("ender_anchor", side, front, provider.modLoc("block/ender_anchor_top"));
        ModelFile lit = provider.models().orientable("ender_anchor_lit", side, front, provider.modLoc("block/ender_anchor_top_lit"));
        provider.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.builder()
                .modelFile(state.getValue(MachineBlock.LIT) ? lit : unlit)
                .rotationY(ModelRotations.rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        provider.simpleBlockItem(block, unlit);
    }
}
