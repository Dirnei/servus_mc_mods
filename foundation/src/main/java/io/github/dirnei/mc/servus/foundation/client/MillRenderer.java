package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillParts;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Turns the Mill's grinding wheel (hub, spokes and rim segments) about the
 * vertical axis from the flywheel its block entity runs on the client: smooth
 * while driven, coasting to a stop after. Also draws the side markers.
 */
public class MillRenderer implements BlockEntityRenderer<MillBlockEntity, MillRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> HUB_MODEL = ModelKeys.key("block/mill_hub");
    public static final StandaloneModelKey<BlockStateModelPart> SPOKE_MODEL = ModelKeys.key("block/mill_spoke");
    public static final StandaloneModelKey<BlockStateModelPart> RIM_MODEL = ModelKeys.key("block/mill_rim");

    private final SideMarkerRenderer<MillBlockEntity> markers;

    /** The wheel's angle of one frame and the side markers. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State markers = new SideMarkerRenderer.State();
        float angle;
    }

    public MillRenderer(BlockEntityRendererProvider.Context context) {
        markers = new SideMarkerRenderer<>(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MillBlockEntity mill, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(mill, state, partialTick, cameraPosition, breakProgress);
        markers.extractRenderState(mill, state.markers, partialTick, cameraPosition, breakProgress);
        state.angle = mill.wheel().angle(partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        markers.submit(state.markers, poseStack, collector, camera);
        int light = state.lightCoords;
        ModelPoses.rotateAbout(poseStack, MillParts.AXIS_X, 0, MillParts.AXIS_Z, Axis.YP, state.angle,
                () -> wheel(poseStack, collector, light));
    }

    /** Hub, spokes and rim segments; each further rim segment a step lower so overlapping tops never share a plane. */
    private static void wheel(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        ModelKeys.submit(HUB_MODEL, poseStack, collector, light);
        for (int i = 0; i < MillParts.SPOKES; i++) {
            ModelPoses.rotateAbout(poseStack, MillParts.AXIS_X, 0, MillParts.AXIS_Z, Axis.YP, 360.0F * i / MillParts.SPOKES,
                    () -> ModelKeys.submit(SPOKE_MODEL, poseStack, collector, light));
        }
        for (int i = 0; i < MillParts.RIM_SEGMENTS; i++) {
            float depth = -i * MillParts.DEPTH_STEP / 16.0F;
            ModelPoses.rotateAbout(poseStack, MillParts.AXIS_X, 0, MillParts.AXIS_Z, Axis.YP, 360.0F * i / MillParts.RIM_SEGMENTS,
                    () -> ModelPoses.translated(poseStack, 0, depth, 0, () -> ModelKeys.submit(RIM_MODEL, poseStack, collector, light)));
        }
    }
}
