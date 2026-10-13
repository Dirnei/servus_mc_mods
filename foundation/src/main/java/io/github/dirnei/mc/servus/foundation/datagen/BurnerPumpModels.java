package io.github.dirnei.mc.servus.foundation.datagen;

import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.blockModel;
import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.box;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpParts;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * The Burner Pump's models: a lower half (stone brick plinth, brick firebox
 * with an oxidized copper fire door, drum, rod socket) per lit state, an upper
 * half (drum top, cylinder, post, chimney), the moving beam, pump rod and
 * piston rod for the renderer, and an item model with the whole engine.
 * Everything is built facing north from {@link BurnerPumpParts}.
 */
final class BurnerPumpModels {

    private static final String NAME = "burner_pump";

    private BurnerPumpModels() {
    }

    static void burnerPump(BlockStateProvider provider) {
        Block block = MachineContent.BURNER_PUMP.get();
        ModelFile unlit = lower(provider, NAME + "_lower", "block/" + NAME + "_front");
        ModelFile lit = lower(provider, NAME + "_lower_lit", "block/" + NAME + "_front_lit");
        BlockModelBuilder upper = base(provider, NAME + "_upper");
        addUpper(upper, 0);
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int rotation = BeamPumpBlock.quarters(facing) * 90;
            for (boolean isLit : new boolean[] {false, true}) {
                builder.part().modelFile(isLit ? lit : unlit).rotationY(rotation).addModel()
                        .condition(BeamPumpBlock.HALF, DoubleBlockHalf.LOWER)
                        .condition(MachineBlock.FACING, facing).condition(MachineBlock.LIT, isLit).end();
            }
            builder.part().modelFile(upper).rotationY(rotation).addModel()
                    .condition(BeamPumpBlock.HALF, DoubleBlockHalf.UPPER).condition(MachineBlock.FACING, facing).end();
        }
        MachineModels.pumpStubParts(provider, builder);
        box(base(provider, NAME + "_beam"), BeamPumpParts.BEAM, 0, "#wood", "#wood");
        MachineModels.rod(base(provider, NAME + "_rod"), BeamPumpParts.ROD, 0, "#iron");
        MachineModels.rod(base(provider, NAME + "_piston"), BurnerPumpParts.PISTON, 0, "#iron");
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder base(BlockStateProvider provider, String name) {
        return blockModel(provider, name)
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
        box(model, BeamPumpParts.BASE, dy, "#stone", "#stone");
        // The front keeps the box UVs so its bricks line up with the vanilla bricks on the sides.
        box(model, BurnerPumpParts.FIREBOX, dy, "#brick", "#oxidized_cut")
                .face(Direction.NORTH).texture("#front").end();
        box(model, BurnerPumpParts.DRUM_LOWER, dy, "#copper", "#copper");
        box(model, BeamPumpParts.SOCKET, dy, "#oxidized", "#oxidized");
    }

    private static void addUpper(BlockModelBuilder model, float dy) {
        box(model, BurnerPumpParts.DRUM_UPPER, dy, "#copper", "#copper");
        box(model, BurnerPumpParts.CYLINDER, dy, "#copper", "#iron");
        box(model, BeamPumpParts.POST, dy, "#wood", "#wood");
        box(model, BurnerPumpParts.CHIMNEY, dy, "#brick", "#brick");
    }

    /** Both halves stacked with the beam level. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = base(provider, NAME + "_item").texture("front", provider.modLoc("block/" + NAME + "_front"));
        addLower(model, 0);
        addUpper(model, 16);
        box(model, BeamPumpParts.BEAM, 16, "#wood", "#wood");
        float[] rod = BeamPumpParts.ROD;
        model.element().from(rod[0], rod[1], rod[2]).to(rod[3], rod[4], rod[5])
                .allFaces((direction, face) -> face.texture("#iron").uvs(7, 0, 8, 16));
        MachineModels.twoBlockDisplay(model);
        return model;
    }
}
