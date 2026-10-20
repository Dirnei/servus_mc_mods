package io.github.dirnei.mc.servus.foundation.datagen;

import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.blockModel;
import static io.github.dirnei.mc.servus.foundation.datagen.MachineModels.box;

import io.github.dirnei.mc.servus.foundation.datagen.model.BlockModelBuilder;
import io.github.dirnei.mc.servus.foundation.datagen.model.BlockStateProvider;
import io.github.dirnei.mc.servus.foundation.datagen.model.ConfiguredModel;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelFile;
import io.github.dirnei.mc.servus.foundation.datagen.model.ModelRotations;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorParts;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlock;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankParts;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The models of the drivers, built facing north: the Hand Crank (flange and
 * bearing, with shaft, arm and handle as one turning model) and the Electric
 * Motor (feet, body, windings, terminal, front plate and grille, with the
 * shaft and coupling and the fan as turning models). Item models hold every
 * part at rest.
 */
final class DriveModels {

    private DriveModels() {
    }

    static void handCrank(BlockStateProvider provider) {
        Block block = MachineContent.HAND_CRANK.get();
        BlockModelBuilder model = crankPart(provider, "hand_crank");
        addCrankStatic(model);
        facingStates(provider, block, HandCrankBlock.FACING, model);
        addCrankHandle(crankPart(provider, "hand_crank_handle"));
        BlockModelBuilder item = crankPart(provider, "hand_crank_item");
        addCrankStatic(item);
        addCrankHandle(item);
        provider.simpleBlockItem(block, item);
    }

    static void electricMotor(BlockStateProvider provider) {
        Block block = MachineContent.ELECTRIC_MOTOR.get();
        BlockModelBuilder model = motorPart(provider, "electric_motor");
        addMotorStatic(model);
        facingStates(provider, block, MachineBlock.FACING, model);
        addCoupling(motorPart(provider, "electric_motor_coupling"));
        addFan(motorPart(provider, "electric_motor_fan"));
        BlockModelBuilder item = motorPart(provider, "electric_motor_item");
        addMotorStatic(item);
        addCoupling(item);
        addFan(item);
        provider.simpleBlockItem(block, item);
    }

    /** One model for every state, turned to the facing. */
    private static void facingStates(BlockStateProvider provider, Block block, EnumProperty<Direction> facing, ModelFile model) {
        provider.getVariantBuilder(block).forAllStates((BlockState state) -> ConfiguredModel.builder()
                .modelFile(model)
                .rotationY(ModelRotations.rotationY(state.getValue(facing)))
                .build());
    }

    private static BlockModelBuilder crankPart(BlockStateProvider provider, String name) {
        return blockModel(provider, name)
                .texture("particle", provider.modLoc("block/heavy_iron"))
                .texture("heavy", provider.modLoc("block/heavy_iron"))
                .texture("steel", provider.modLoc("block/electric_pump_steel"))
                .texture("wood", provider.mcLoc("block/oak_planks"));
    }

    private static void addCrankStatic(BlockModelBuilder model) {
        box(model, HandCrankParts.FLANGE, 0, "#heavy", "#heavy");
        box(model, HandCrankParts.BEARING, 0, "#heavy", "#heavy");
    }

    /** Shaft, arm and wooden handle: they turn together. */
    private static void addCrankHandle(BlockModelBuilder model) {
        box(model, HandCrankParts.SHAFT, 0, "#steel", "#steel");
        box(model, HandCrankParts.ARM, 0, "#heavy", "#heavy");
        box(model, HandCrankParts.HANDLE, 0, "#wood", "#wood");
    }

    private static BlockModelBuilder motorPart(BlockStateProvider provider, String name) {
        return blockModel(provider, name)
                .texture("particle", provider.modLoc("block/electric_pump_steel"))
                .texture("heavy", provider.modLoc("block/heavy_iron"))
                .texture("steel", provider.modLoc("block/electric_pump_steel"))
                .texture("copper", provider.mcLoc("block/copper_block"))
                .texture("terminal", provider.modLoc("block/electric_pump_terminal"))
                .texture("grille", provider.modLoc("block/electric_motor_grille"))
                .texture("pulley", provider.modLoc("block/electric_pump_pulley"))
                .texture("fan", provider.modLoc("block/electric_pump_fan"));
    }

    private static void addMotorStatic(BlockModelBuilder model) {
        box(model, ElectricMotorParts.FEET, 0, "#heavy", "#heavy");
        box(model, ElectricMotorParts.BODY, 0, "#steel", "#steel");
        for (float[] winding : ElectricMotorParts.WINDINGS) {
            box(model, winding, 0, "#copper", "#copper");
        }
        box(model, ElectricMotorParts.TERMINAL, 0, "#terminal", "#terminal");
        box(model, ElectricMotorParts.FRONT_PLATE, 0, "#heavy", "#heavy");
        box(model, ElectricMotorParts.GRILLE, 0, "#steel", "#steel").face(Direction.SOUTH).texture("#grille").end();
    }

    /** The shaft and the coupling with the pulley's spokes on its front, so its turning shows. */
    private static void addCoupling(BlockModelBuilder model) {
        box(model, ElectricMotorParts.SHAFT, 0, "#steel", "#steel");
        box(model, ElectricMotorParts.COUPLING, 0, "#steel", "#steel").face(Direction.NORTH).texture("#pulley").uvs(0, 0, 16, 16).end();
    }

    /** The fan plate, painted on both flat faces. */
    private static void addFan(BlockModelBuilder model) {
        float[] f = ElectricMotorParts.FAN;
        model.element().from(f[0], f[1], f[2]).to(f[3], f[4], f[5])
                .face(Direction.NORTH).texture("#fan").uvs(0, 0, 16, 16).end()
                .face(Direction.SOUTH).texture("#fan").uvs(0, 0, 16, 16).end();
    }
}
