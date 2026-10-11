package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;

/**
 * The Burner Pump's models: a lower half (stone brick plinth, brick firebox
 * with an oxidized copper fire door, drum, rod socket) per lit
 * state, an upper half (drum top, cylinder, post, chimney), the moving beam,
 * pump rod and piston rod for the renderer, and an item model with the whole
 * engine. Everything is built facing north from {@link BurnerPumpParts}.
 */
final class BurnerPumpModels {

    private static final String NAME = "burner_pump";

    private BurnerPumpModels() {
    }

    static void burnerPump(BlockStateProvider provider) {
        Block block = MachineContent.BURNER_PUMP.get();
        ModelFile unlit = lower(provider, NAME + "_lower", "block/" + NAME + "_front");
        ModelFile lit = lower(provider, NAME + "_lower_lit", "block/" + NAME + "_front_lit");
        ModelFile upper = upper(provider);
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int rotation = BurnerPumpBlock.quarters(facing) * 90;
            for (boolean isLit : new boolean[] {false, true}) {
                builder.part().modelFile(isLit ? lit : unlit).rotationY(rotation).addModel()
                        .condition(BurnerPumpBlock.HALF, DoubleBlockHalf.LOWER)
                        .condition(MachineBlock.FACING, facing).condition(MachineBlock.LIT, isLit).end();
            }
            builder.part().modelFile(upper).rotationY(rotation).addModel()
                    .condition(BurnerPumpBlock.HALF, DoubleBlockHalf.UPPER).condition(MachineBlock.FACING, facing).end();
        }
        for (Map.Entry<Direction, IntegerProperty> stub : PumpBlock.STUBS.entrySet()) {
            for (int size = 1; size <= PumpBlock.MAX_STUB; size++) {
                builder.part().modelFile(MachineDataParts.pumpStub(provider, stub.getKey(), size)).addModel()
                        .condition(stub.getValue(), size).condition(BurnerPumpBlock.HALF, DoubleBlockHalf.LOWER).end();
            }
        }
        beam(provider);
        rod(provider, NAME + "_rod", BurnerPumpParts.ROD);
        rod(provider, NAME + "_piston", BurnerPumpParts.PISTON);
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder base(BlockStateProvider provider, String name) {
        return provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.mcLoc("block/bricks"))
                .texture("stone", provider.mcLoc("block/stone_bricks"))
                .texture("brick", provider.mcLoc("block/bricks"))
                .texture("copper", provider.mcLoc("block/copper_block"))
                .texture("oxidized", provider.mcLoc("block/oxidized_copper"))
                .texture("oxidized_cut", provider.mcLoc("block/oxidized_cut_copper"))
                .texture("iron", provider.mcLoc("block/iron_block"))
                .texture("wood", provider.mcLoc("block/stripped_dark_oak_log"));
    }

    private static ModelFile lower(BlockStateProvider provider, String name, String front) {
        BlockModelBuilder model = base(provider, name).texture("front", provider.modLoc(front));
        addLower(model, 0);
        return model;
    }

    private static void addLower(BlockModelBuilder model, float dy) {
        box(model, BurnerPumpParts.BASE, dy, "#stone", "#stone");
        // The front keeps the box UVs so its bricks line up with the vanilla bricks on the sides.
        box(model, BurnerPumpParts.FIREBOX, dy, "#brick", "#oxidized_cut")
                .face(Direction.NORTH).texture("#front").end();
        box(model, BurnerPumpParts.DRUM_LOWER, dy, "#copper", "#copper");
        box(model, BurnerPumpParts.SOCKET, dy, "#oxidized", "#oxidized");
    }

    private static ModelFile upper(BlockStateProvider provider) {
        BlockModelBuilder model = base(provider, NAME + "_upper");
        addUpper(model, 0);
        return model;
    }

    private static void addUpper(BlockModelBuilder model, float dy) {
        box(model, BurnerPumpParts.DRUM_UPPER, dy, "#copper", "#copper");
        box(model, BurnerPumpParts.CYLINDER, dy, "#copper", "#iron");
        box(model, BurnerPumpParts.POST, dy, "#wood", "#wood");
        box(model, BurnerPumpParts.CHIMNEY, dy, "#brick", "#brick");
    }

    /** The beam in the upper half's frame, level; the renderer rocks it about the pivot. */
    private static void beam(BlockStateProvider provider) {
        BlockModelBuilder model = base(provider, NAME + "_beam");
        box(model, BurnerPumpParts.BEAM, 0, "#wood", "#wood");
    }

    /** A thin iron rod; its sides take a strip of the texture, since it is taller than a block. */
    private static void rod(BlockStateProvider provider, String name, float[] rod) {
        BlockModelBuilder model = base(provider, name);
        model.element().from(rod[0], rod[1], rod[2]).to(rod[3], rod[4], rod[5])
                .allFaces((direction, face) -> face.texture("#iron").uvs(7, 0, 8, 16))
                .face(Direction.UP).uvs(7, 7, 8, 8).end()
                .face(Direction.DOWN).uvs(7, 7, 8, 8).end();
    }

    /** Both halves stacked, the beam level, scaled down so the two block engine fits a slot. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = base(provider, NAME + "_item").texture("front", provider.modLoc("block/" + NAME + "_front"));
        addLower(model, 0);
        addUpper(model, 16);
        box(model, BurnerPumpParts.BEAM, 16, "#wood", "#wood");
        float[] rod = BurnerPumpParts.ROD;
        model.element().from(rod[0], rod[1], rod[2]).to(rod[3], rod[4], rod[5])
                .allFaces((direction, face) -> face.texture("#iron").uvs(7, 0, 8, 16));
        // The model is 32 pixels tall: scale it down and lower it by its extra half.
        display(model, ItemDisplayContext.GUI, 30, 225, 0.4F);
        display(model, ItemDisplayContext.GROUND, 0, 0, 0.2F);
        display(model, ItemDisplayContext.FIXED, 0, 0, 0.4F);
        display(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 0, 225, 0.3F);
        return model;
    }

    private static void display(BlockModelBuilder model, ItemDisplayContext context, float rotX, float rotY, float scale) {
        model.transforms().transform(context).rotation(rotX, rotY, 0).translation(0, -8 * scale, 0).scale(scale).end();
    }

    /**
     * A box with the UVs vanilla would pick for it at its own height, moved up
     * by {@code dy}: the side texture on the sides, {@code ends} on top and bottom.
     */
    private static BlockModelBuilder.ElementBuilder box(BlockModelBuilder model, float[] b, float dy, String sides, String ends) {
        BlockModelBuilder.ElementBuilder element = model.element().from(b[0], b[1] + dy, b[2]).to(b[3], b[4] + dy, b[5]);
        element.face(Direction.DOWN).texture(ends).uvs(b[0], 16 - b[5], b[3], 16 - b[2]).end();
        element.face(Direction.UP).texture(ends).uvs(b[0], b[2], b[3], b[5]).end();
        element.face(Direction.NORTH).texture(sides).uvs(16 - b[3], 16 - b[4], 16 - b[0], 16 - b[1]).end();
        element.face(Direction.SOUTH).texture(sides).uvs(b[0], 16 - b[4], b[3], 16 - b[1]).end();
        element.face(Direction.WEST).texture(sides).uvs(b[2], 16 - b[4], b[5], 16 - b[1]).end();
        element.face(Direction.EAST).texture(sides).uvs(16 - b[5], 16 - b[4], 16 - b[2], 16 - b[1]).end();
        return element;
    }
}
