package io.github.dirnei.mc.servus.foundation.datagen;

import static io.github.dirnei.mc.servus.foundation.datagen.BurnerPumpModels.box;
import static io.github.dirnei.mc.servus.foundation.datagen.BurnerPumpModels.display;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.MultiPartBlockStateBuilder;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.PumpBlock;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The Electric Pump's models: a lower half (dark stone base, motor housing with
 * copper windings, terminal box, motor cap, rod socket), an upper half (cap,
 * post, fan grille), the moving beam, pump rod, con rod, crank wheel and fan for
 * the renderer, and an item model with the whole engine. Everything is built
 * facing north from {@link BeamPumpParts} and {@link ElectricPumpParts}.
 */
final class ElectricPumpModels {

    private static final String NAME = "electric_pump";

    private ElectricPumpModels() {
    }

    static void electricPump(BlockStateProvider provider) {
        Block block = MachineContent.ELECTRIC_PUMP.get();
        BlockModelBuilder lower = part(provider, NAME + "_lower", false);
        addLower(lower, 0);
        BlockModelBuilder upper = part(provider, NAME + "_upper", true);
        addUpper(upper, 0);
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(block);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int rotation = BeamPumpBlock.quarters(facing) * 90;
            builder.part().modelFile(lower).rotationY(rotation).addModel()
                    .condition(BeamPumpBlock.HALF, DoubleBlockHalf.LOWER).condition(MachineBlock.FACING, facing).end();
            builder.part().modelFile(upper).rotationY(rotation).addModel()
                    .condition(BeamPumpBlock.HALF, DoubleBlockHalf.UPPER).condition(MachineBlock.FACING, facing).end();
        }
        for (Map.Entry<Direction, IntegerProperty> stub : PumpBlock.STUBS.entrySet()) {
            for (int size = 1; size <= PumpBlock.MAX_STUB; size++) {
                builder.part().modelFile(MachineDataParts.pumpStub(provider, stub.getKey(), size)).addModel()
                        .condition(stub.getValue(), size).condition(BeamPumpBlock.HALF, DoubleBlockHalf.LOWER).end();
            }
        }
        addBeam(part(provider, NAME + "_beam", false), 0);
        addRod(part(provider, NAME + "_rod", false), BeamPumpParts.ROD, 0);
        addRod(part(provider, NAME + "_con_rod", false), ElectricPumpParts.CON_ROD, 0);
        addWheel(part(provider, NAME + "_wheel", true), 0);
        addFan(part(provider, NAME + "_fan", true), 0);
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder part(BlockStateProvider provider, String name, boolean cutout) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/" + NAME + "_housing"))
                .texture("stone", provider.mcLoc("block/polished_blackstone"))
                .texture("copper", provider.mcLoc("block/copper_block"))
                .texture("oxidized", provider.mcLoc("block/oxidized_copper"))
                .texture("iron", provider.mcLoc("block/iron_block"))
                .texture("housing", provider.modLoc("block/" + NAME + "_housing"))
                .texture("terminal", provider.modLoc("block/" + NAME + "_terminal"))
                .texture("grille", provider.modLoc("block/" + NAME + "_grille"))
                .texture("fan", provider.modLoc("block/" + NAME + "_fan"))
                .texture("wheel", provider.modLoc("block/" + NAME + "_wheel"));
        return cutout ? model.renderType("cutout") : model;
    }

    private static void addLower(BlockModelBuilder model, float dy) {
        box(model, BeamPumpParts.BASE, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.HOUSING, dy, "#housing", "#housing");
        for (float[] winding : ElectricPumpParts.WINDINGS) {
            box(model, winding, dy, "#copper", "#copper");
        }
        box(model, ElectricPumpParts.TERMINAL, dy, "#terminal", "#terminal");
        box(model, ElectricPumpParts.MOTOR_CAP, dy, "#stone", "#stone");
        box(model, BeamPumpParts.SOCKET, dy, "#oxidized", "#oxidized");
    }

    /** Cap, post and the fan grille: slats with open gaps in front of a dark box, the fan spins between them. */
    private static void addUpper(BlockModelBuilder model, float dy) {
        box(model, ElectricPumpParts.CAP, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.POST_FOOT, dy, "#iron", "#iron");
        box(model, BeamPumpParts.POST, dy, "#iron", "#iron");
        box(model, ElectricPumpParts.GRILLE_BOX, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.GRILLE_FRAME, dy, "#stone", "#stone").face(Direction.WEST).texture("#grille").end();
    }

    private static void addBeam(BlockModelBuilder model, float dy) {
        box(model, BeamPumpParts.BEAM, dy, "#iron", "#stone");
    }

    /** A thin iron rod; its sides take a strip of the texture, since it can be taller than a block. */
    private static void addRod(BlockModelBuilder model, float[] rod, float dy) {
        model.element().from(rod[0], rod[1] + dy, rod[2]).to(rod[3], rod[4] + dy, rod[5])
                .allFaces((direction, face) -> face.texture("#iron").uvs(7, 0, 8, 16))
                .face(Direction.UP).uvs(7, 7, 8, 8).end()
                .face(Direction.DOWN).uvs(7, 7, 8, 8).end();
    }

    /** The wheel disc shows only its two flat faces, so it stays round while it turns; the pin turns with it. */
    private static void addWheel(BlockModelBuilder model, float dy) {
        float[] w = ElectricPumpParts.WHEEL;
        model.element().from(w[0], w[1] + dy, w[2]).to(w[3], w[4] + dy, w[5])
                .face(Direction.NORTH).texture("#wheel").uvs(1.5F, 1.5F, 13.5F, 13.5F).end()
                .face(Direction.SOUTH).texture("#wheel").uvs(1.5F, 1.5F, 13.5F, 13.5F).end();
        box(model, ElectricPumpParts.PIN, dy, "#iron", "#iron");
    }

    /** The fan plate, painted on both flat faces. */
    private static void addFan(BlockModelBuilder model, float dy) {
        float[] f = ElectricPumpParts.FAN;
        model.element().from(f[0], f[1] + dy, f[2]).to(f[3], f[4] + dy, f[5])
                .face(Direction.WEST).texture("#fan").uvs(0, 0, 16, 16).end()
                .face(Direction.EAST).texture("#fan").uvs(0, 0, 16, 16).end();
    }

    /** Both halves stacked with every moving part at rest, scaled down so the two block engine fits a slot. */
    private static ModelFile item(BlockStateProvider provider) {
        BlockModelBuilder model = part(provider, NAME + "_item", true);
        addLower(model, 0);
        addUpper(model, 16);
        addBeam(model, 16);
        addRod(model, BeamPumpParts.ROD, 0);
        addRod(model, ElectricPumpParts.CON_ROD, 16);
        addWheel(model, 16);
        addFan(model, 16);
        display(model, ItemDisplayContext.GUI, 30, 225, 0.4F);
        display(model, ItemDisplayContext.GROUND, 0, 0, 0.2F);
        display(model, ItemDisplayContext.FIXED, 0, 0, 0.4F);
        display(model, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, 75, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, 0, 45, 0.3F);
        display(model, ItemDisplayContext.FIRST_PERSON_LEFT_HAND, 0, 225, 0.3F);
        return model;
    }
}
