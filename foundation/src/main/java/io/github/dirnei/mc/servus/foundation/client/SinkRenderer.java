package io.github.dirnei.mc.servus.foundation.client;

import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.sink.SinkLayout;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the one big fan of a formed Sink behind the grate of its 2x2 front,
 * spinning while the Sink is active and faster with a taller chimney. Only the
 * controller draws; the other parts render nothing.
 */
public class SinkRenderer implements BlockEntityRenderer<SinkBlockEntity, SinkRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> FAN_MODEL = ModelKeys.key("block/sink_fan");

    /** Degrees per tick without a chimney, and what each Chimney block adds. */
    private static final float BASE_SPEED = 6.0F;
    private static final float SPEED_PER_HEIGHT = 3.0F;
    /** Heights above this spin no faster, so very tall columns stay readable. */
    private static final int MAX_SPEED_HEIGHT = 8;

    /** The fan of one frame; nothing is drawn unless this is a formed controller. */
    public static final class State extends BlockEntityRenderState {
        boolean draw;
        float angle;
        int light;
        Direction facing = Direction.NORTH;
    }

    public SinkRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static boolean isFormedController(BlockState state) {
        return SinkBlock.isFormed(state) && state.getValue(SinkBlock.PART) == SinkLayout.CONTROLLER;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SinkBlockEntity sink, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(sink, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = sink.getBlockState();
        state.draw = isFormedController(blockState);
        if (!state.draw) {
            return;
        }
        state.angle = fanAngle(sink, blockState, partialTick);
        state.facing = blockState.getValue(SinkBlock.FACING);
        // The fan sits in the middle of the front, where all four front parts meet: light it from there.
        state.light = state.lightCoords;
        if (sink.getLevel() != null) {
            BlockPos centre = SinkBlock.partPos(sink.getBlockPos(), state.facing, SinkLayout.RIGHT | SinkLayout.UP).relative(state.facing);
            state.light = LevelRenderer.getLightCoords(sink.getLevel(), centre);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.draw) {
            return;
        }
        poseStack.pushPose();
        // The block models are built facing north; turn like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((state.facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        // In that frame the right parts lie towards -x, so the front's centre is the controller's
        // upper west edge. Spin about the axis through it; the fan model is centred on (8, 8).
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.angle));
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        ModelKeys.submit(FAN_MODEL, poseStack, collector, state.light);
        poseStack.popPose();
    }

    /** The whole cube, so the fan stays visible while the controller block itself is out of view. */
    @Override
    public AABB getRenderBoundingBox(SinkBlockEntity sink) {
        BlockState state = sink.getBlockState();
        if (!isFormedController(state)) {
            return new AABB(sink.getBlockPos());
        }
        BlockPos far = SinkBlock.partPos(sink.getBlockPos(), state.getValue(SinkBlock.FACING), SinkLayout.PARTS - 1);
        return new AABB(sink.getBlockPos()).minmax(new AABB(far));
    }

    /** Advances the fan while active and keeps the last angle while idle. */
    private static float fanAngle(SinkBlockEntity sink, BlockState state, float partialTick) {
        if (sink.getLevel() == null) {
            return sink.fanAngle;
        }
        double now = sink.getLevel().getGameTime() + (double) partialTick;
        float elapsed = sink.fanTime < 0 ? 0 : (float) Math.max(0, now - sink.fanTime);
        sink.fanTime = now;
        if (state.getValue(SinkBlock.ACTIVE)) {
            float speed = BASE_SPEED + SPEED_PER_HEIGHT * Math.min(sink.chimneyHeight(), MAX_SPEED_HEIGHT);
            sink.fanAngle = (sink.fanAngle + elapsed * speed) % 360.0F;
        }
        return sink.fanAngle;
    }
}
