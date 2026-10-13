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
import io.github.dirnei.mc.servus.foundation.machine.pump.BeltDrive;
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
 * copper windings, terminal box, motor cap, motor shaft, rod socket), an upper
 * half (cap, post, bearing on its column, fan grille), the moving beam, pump
 * rod, con rod, crank hub with its pin, spoke, rim segment, axle with its
 * pulley, motor pulley,
 * belt strand, lacing mark and fan for the renderer, and an item model with
 * the whole engine. Everything is built
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
        addWheel(part(provider, NAME + "_wheel", false), 0);
        box(part(provider, NAME + "_spoke", false), ElectricPumpParts.SPOKE, 0, "#steel", "#steel");
        addFan(part(provider, NAME + "_fan", true), 0);
        box(part(provider, NAME + "_rim", false), ElectricPumpParts.RIM_SEGMENT, 0, "#steel", "#steel");
        addAxle(part(provider, NAME + "_axle", true), 0);
        disc(part(provider, NAME + "_motor_pulley", true), ElectricPumpParts.MOTOR_PULLEY, 0);
        strap(part(provider, NAME + "_belt", false), ElectricPumpParts.BELT, "#belt");
        strap(part(provider, NAME + "_belt_mark", false), ElectricPumpParts.BELT_MARK, "#stone");
        provider.simpleBlockItem(block, item(provider));
    }

    private static BlockModelBuilder part(BlockStateProvider provider, String name, boolean cutout) {
        BlockModelBuilder model = provider.models().withExistingParent(name, provider.mcLoc("block/block"))
                .texture("particle", provider.modLoc("block/" + NAME + "_housing"))
                .texture("stone", provider.mcLoc("block/polished_blackstone"))
                .texture("copper", provider.mcLoc("block/copper_block"))
                .texture("oxidized", provider.mcLoc("block/oxidized_copper"))
                .texture("steel", provider.modLoc("block/" + NAME + "_steel"))
                .texture("housing", provider.modLoc("block/" + NAME + "_housing"))
                .texture("terminal", provider.modLoc("block/" + NAME + "_terminal"))
                .texture("grille", provider.modLoc("block/" + NAME + "_grille"))
                .texture("fan", provider.modLoc("block/" + NAME + "_fan"))
                .texture("belt", provider.modLoc("block/" + NAME + "_belt"))
                .texture("pulley", provider.modLoc("block/" + NAME + "_pulley"));
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
        box(model, ElectricPumpParts.MOTOR_SHAFT, dy, "#steel", "#steel");
    }

    /** Cap, post, the axle's bearing on its column and the fan grille: slats with open gaps in front of a dark box, the fan spins between them. */
    private static void addUpper(BlockModelBuilder model, float dy) {
        box(model, ElectricPumpParts.CAP, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.POST_FOOT, dy, "#steel", "#steel");
        box(model, BeamPumpParts.POST, dy, "#steel", "#steel");
        box(model, ElectricPumpParts.GRILLE_BOX, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.GRILLE_FRAME, dy, "#stone", "#stone").face(Direction.WEST).texture("#grille").end();
        box(model, ElectricPumpParts.BEARING_COLUMN, dy, "#stone", "#stone");
        box(model, ElectricPumpParts.BEARING, dy, "#housing", "#housing");
    }

    /** The iron axle and the pulley on its back end; both turn with the crank. */
    private static void addAxle(BlockModelBuilder model, float dy) {
        box(model, ElectricPumpParts.AXLE, dy, "#steel", "#steel");
        disc(model, ElectricPumpParts.AXLE_PULLEY, dy);
    }

    /** A pulley disc showing its spokes on both flat faces, so it stays round while it turns. */
    private static void disc(BlockModelBuilder model, float[] d, float dy) {
        model.element().from(d[0], d[1] + dy, d[2]).to(d[3], d[4] + dy, d[5])
                .face(Direction.NORTH).texture("#pulley").uvs(0, 0, 16, 16).end()
                .face(Direction.SOUTH).texture("#pulley").uvs(0, 0, 16, 16).end();
    }

    /**
     * A part modelled at crank angle 0, turned about the crank axle by an
     * element rotation, for the item model, which cannot turn it at run time.
     * Minecraft 26.1 takes any angle here.
     */
    private static void turned(BlockModelBuilder model, float[] b, float dy, float degrees) {
        box(model, b, dy, "#steel", "#steel").rotation()
                .origin(ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y + dy, (b[2] + b[5]) / 2)
                .axis(Direction.Axis.Z).angle(degrees).end();
    }

    /** The wheel's spokes and rim for the item model, as the renderer turns them in the world. */
    private static void addItemSpokesAndRim(BlockModelBuilder model, float dy) {
        for (int i = 0; i < ElectricPumpParts.SPOKES; i++) {
            turned(model, ElectricPumpParts.SPOKE, dy, 360.0F * i / ElectricPumpParts.SPOKES);
        }
        for (int i = 0; i < ElectricPumpParts.RIM_SEGMENTS; i++) {
            turned(model, ElectricPumpParts.RIM_SEGMENT, dy, 360.0F * i / ElectricPumpParts.RIM_SEGMENTS);
        }
    }

    /**
     * The belt for the item model: both strands and the arcs round the
     * pulleys, each piece a strap stood up at its middle and turned along it.
     */
    private static void addItemBelt(BlockModelBuilder model) {
        BeltDrive drive = ElectricPumpParts.DRIVE;
        int segments = ElectricPumpParts.BELT_ARC_SEGMENTS;
        for (int side = 0; side < 2; side++) {
            float[] s = drive.strand(side);
            beltPiece(model, s[0], s[1], s[2], s[3], 0);
        }
        for (int pulley = 0; pulley < 2; pulley++) {
            for (int i = 0; i < segments; i++) {
                float[] a = drive.arcPoint(pulley, (double) i / segments);
                float[] b = drive.arcPoint(pulley, (double) (i + 1) / segments);
                beltPiece(model, a[0], a[1], b[0], b[1], 0.06F);
            }
        }
    }

    /** One straight belt piece from (x1, y1) to (x2, y2), reaching {@code overlap} past each end so the joints stay closed. */
    private static void beltPiece(BlockModelBuilder model, float x1, float y1, float x2, float y2, float overlap) {
        float half = ElectricPumpParts.BELT_T / 2;
        float mx = (x1 + x2) / 2;
        float my = (y1 + y2) / 2;
        float reach = (float) Math.hypot(x2 - x1, y2 - y1) / 2 + overlap;
        float degrees = (float) Math.toDegrees(Math.atan2(-(x2 - x1), y2 - y1));
        strap(model, new float[] {mx - half, my - reach, ElectricPumpParts.BELT_Z1, mx + half, my + reach, ElectricPumpParts.BELT_Z2}, "#belt")
                .rotation().origin(mx, my, (ElectricPumpParts.BELT_Z1 + ElectricPumpParts.BELT_Z2) / 2)
                .axis(Direction.Axis.Z).angle(degrees).end();
    }

    /** A thin strap or mark with fixed UVs, since it may reach below or above its block. */
    private static BlockModelBuilder.ElementBuilder strap(BlockModelBuilder model, float[] b, String texture) {
        return model.element().from(b[0], b[1], b[2]).to(b[3], b[4], b[5])
                .allFaces((direction, face) -> face.texture(texture).uvs(2, 0, 14, 16))
                .face(Direction.UP).uvs(2, 7, 14, 8).end()
                .face(Direction.DOWN).uvs(2, 7, 14, 8).end();
    }

    private static void addBeam(BlockModelBuilder model, float dy) {
        box(model, BeamPumpParts.BEAM, dy, "#steel", "#stone");
    }

    /** A thin steel rod; its sides take a strip of the texture, since it can be taller than a block. */
    private static void addRod(BlockModelBuilder model, float[] rod, float dy) {
        model.element().from(rod[0], rod[1] + dy, rod[2]).to(rod[3], rod[4] + dy, rod[5])
                .allFaces((direction, face) -> face.texture("#steel").uvs(7, 0, 8, 16))
                .face(Direction.UP).uvs(7, 7, 8, 8).end()
                .face(Direction.DOWN).uvs(7, 7, 8, 8).end();
    }

    /**
     * The crank hub round the axle, drawn twice with the second turned by 45
     * degrees so it reads round, and the crank pin; both turn with the wheel.
     * The spokes and rim segments are their own models, turned by the renderer.
     */
    private static void addWheel(BlockModelBuilder model, float dy) {
        box(model, ElectricPumpParts.HUB, dy, "#steel", "#steel");
        turned(model, ElectricPumpParts.HUB, dy, 45);
        box(model, ElectricPumpParts.PIN, dy, "#steel", "#steel");
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
        addItemSpokesAndRim(model, 16);
        addAxle(model, 16);
        disc(model, ElectricPumpParts.MOTOR_PULLEY, 0);
        addItemBelt(model);
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
