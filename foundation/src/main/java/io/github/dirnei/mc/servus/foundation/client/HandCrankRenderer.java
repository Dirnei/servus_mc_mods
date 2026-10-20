package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlock;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankBlockEntity;
import io.github.dirnei.mc.servus.foundation.drive.HandCrankParts;
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

/** Turns the Hand Crank's shaft, arm and handle from its flywheel: smooth while held, coasting to a stop after. */
public class HandCrankRenderer implements BlockEntityRenderer<HandCrankBlockEntity, HandCrankRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> HANDLE_MODEL = ModelKeys.key("block/hand_crank_handle");

    /** The arm's angle of one frame. */
    public static final class State extends BlockEntityRenderState {
        float angle;
        Direction facing = Direction.NORTH;
    }

    public HandCrankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(HandCrankBlockEntity crank, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crank, state, partialTick, cameraPosition, breakProgress);
        state.angle = crank.armAngle(partialTick);
        state.facing = crank.getBlockState().getValue(HandCrankBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, state.facing);
        ModelPoses.rotateAbout(poseStack, HandCrankParts.AXIS_X, HandCrankParts.AXIS_Y, 0, Axis.ZP, state.angle,
                () -> ModelKeys.submit(HANDLE_MODEL, poseStack, collector, state.lightCoords));
        poseStack.popPose();
    }
}
