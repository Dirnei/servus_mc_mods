package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorBlockEntity;
import io.github.dirnei.mc.servus.foundation.drive.ElectricMotorParts;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Turns the Electric Motor's shaft with its coupling and the fan behind the
 * grille while it works, from the angle its block entity keeps on the client;
 * standing, both keep their pose.
 */
public class ElectricMotorRenderer implements BlockEntityRenderer<ElectricMotorBlockEntity, ElectricMotorRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> COUPLING_MODEL = ModelKeys.key("block/electric_motor_coupling");
    public static final StandaloneModelKey<BlockStateModelPart> FAN_MODEL = ModelKeys.key("block/electric_motor_fan");

    /** The shaft's angle of one frame. */
    public static final class State extends BlockEntityRenderState {
        float angle;
        Direction facing = Direction.NORTH;
    }

    public ElectricMotorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElectricMotorBlockEntity motor, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(motor, state, partialTick, cameraPosition, breakProgress);
        state.angle = motor.spin().angle(partialTick);
        state.facing = motor.getBlockState().getValue(MachineBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, state.facing);
        ModelPoses.rotateAbout(poseStack, ElectricMotorParts.AXIS_X, ElectricMotorParts.AXIS_Y, 0, Axis.ZP, state.angle, () -> {
            ModelKeys.submit(COUPLING_MODEL, poseStack, collector, light);
            ModelKeys.submit(FAN_MODEL, poseStack, collector, light);
        });
        poseStack.popPose();
    }
}
