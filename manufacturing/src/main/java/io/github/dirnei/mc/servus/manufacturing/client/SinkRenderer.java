package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the one big fan of a formed Sink behind the grate of its 2x2 front,
 * spinning while the Sink is active and faster with a taller chimney. Only the
 * controller draws; the other parts render nothing.
 */
public class SinkRenderer implements BlockEntityRenderer<SinkBlockEntity> {

    public static final ModelResourceLocation FAN_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "block/sink_fan"));

    /** Degrees per tick without a chimney, and what each Chimney block adds. */
    private static final float BASE_SPEED = 6.0F;
    private static final float SPEED_PER_HEIGHT = 3.0F;
    /** Heights above this spin no faster, so very tall columns stay readable. */
    private static final int MAX_SPEED_HEIGHT = 8;

    private final BlockEntityRendererProvider.Context context;

    public SinkRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private static boolean isFormedController(BlockState state) {
        return SinkBlock.isFormed(state) && state.getValue(SinkBlock.PART) == SinkLayout.CONTROLLER;
    }

    @Override
    public void render(SinkBlockEntity sink, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = sink.getBlockState();
        if (!isFormedController(state)) {
            return;
        }
        float angle = fanAngle(sink, state, partialTick);
        Direction facing = state.getValue(SinkBlock.FACING);
        BakedModel fan = Minecraft.getInstance().getModelManager().getModel(FAN_MODEL);
        // The fan sits in the middle of the front, where all four front parts meet: light it from there.
        int light = packedLight;
        if (sink.getLevel() != null) {
            BlockPos centre = SinkBlock.partPos(sink.getBlockPos(), facing, SinkLayout.RIGHT | SinkLayout.UP).relative(facing);
            light = LevelRenderer.getLightColor(sink.getLevel(), centre);
        }

        poseStack.pushPose();
        // The block models are built facing north; turn like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        // In that frame the right parts lie towards -x, so the front's centre is the controller's
        // upper west edge. Spin about the axis through it; the fan model is centred on (8, 8).
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        context.getBlockRenderDispatcher().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(RenderType.cutout()), state, fan, 1.0F, 1.0F, 1.0F, light, packedOverlay);
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
