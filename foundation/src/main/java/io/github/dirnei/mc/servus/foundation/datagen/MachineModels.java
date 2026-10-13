package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.Map;
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

    /** Block state rotation Y that turns a north facing model toward {@code facing}. */
    static int rotationY(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360;
    }

    /**
     * A box with the UVs vanilla would pick for it at its own height, moved up
     * by {@code dy}: the side texture on the sides, {@code ends} on top and bottom.
     *
     * @param b {@code {x0, y0, z0, x1, y1, z1}} in pixels
     */
    static BlockModelBuilder.ElementBuilder box(BlockModelBuilder model, float[] b, float dy, String sides, String ends) {
        BlockModelBuilder.ElementBuilder element = model.element().from(b[0], b[1] + dy, b[2]).to(b[3], b[4] + dy, b[5]);
        element.face(Direction.DOWN).texture(ends).uvs(b[0], 16 - b[5], b[3], 16 - b[2]).end();
        element.face(Direction.UP).texture(ends).uvs(b[0], b[2], b[3], b[5]).end();
        element.face(Direction.NORTH).texture(sides).uvs(16 - b[3], 16 - b[4], 16 - b[0], 16 - b[1]).end();
        element.face(Direction.SOUTH).texture(sides).uvs(b[0], 16 - b[4], b[3], 16 - b[1]).end();
        element.face(Direction.WEST).texture(sides).uvs(b[2], 16 - b[4], b[5], 16 - b[1]).end();
        element.face(Direction.EAST).texture(sides).uvs(16 - b[5], 16 - b[4], 16 - b[2], 16 - b[1]).end();
        return element;
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
                .rotationY(rotationY(state.getValue(MachineBlock.FACING)))
                .build());
        provider.simpleBlockItem(block, unlit);
    }
}
