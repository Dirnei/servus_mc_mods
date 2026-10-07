package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.lib.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlock;
import io.github.dirnei.mc.servus.manufacturing.machine.sink.SinkBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the Sink fan in the recess behind the grate, spinning while the Sink is
 * active and faster with a taller chimney, then the side markers.
 */
public class SinkRenderer implements BlockEntityRenderer<SinkBlockEntity> {

    public static final ModelResourceLocation FAN_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "block/sink_fan"));

    /** Degrees per tick without a chimney, and what each Chimney block adds. */
    private static final float BASE_SPEED = 6.0F;
    private static final float SPEED_PER_HEIGHT = 3.0F;
    /** Heights above this spin no faster, so very tall columns stay readable. */
    private static final int MAX_SPEED_HEIGHT = 8;

    private final SideMarkerRenderer<SinkBlockEntity> markers;
    private final BlockEntityRendererProvider.Context context;

    public SinkRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
        this.markers = new SideMarkerRenderer<>(context);
    }

    @Override
    public void render(SinkBlockEntity sink, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = sink.getBlockState();
        float angle = fanAngle(sink, state, partialTick);
        Direction facing = state.hasProperty(SinkBlock.FACING) ? state.getValue(SinkBlock.FACING) : Direction.NORTH;
        BakedModel fan = Minecraft.getInstance().getModelManager().getModel(FAN_MODEL);

        poseStack.pushPose();
        // The fan model is built facing north like the block model; turn it like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        // Spin about the axis through the middle of the front.
        poseStack.translate(0.5F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -0.5F, 0.0F);
        context.getBlockRenderDispatcher().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(RenderType.cutout()), state, fan, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        poseStack.popPose();

        markers.render(sink, partialTick, poseStack, buffers, packedLight, packedOverlay);
    }

    /** Advances the fan while active and keeps the last angle while idle. */
    private static float fanAngle(SinkBlockEntity sink, BlockState state, float partialTick) {
        if (sink.getLevel() == null) {
            return sink.fanAngle;
        }
        double now = sink.getLevel().getGameTime() + (double) partialTick;
        float elapsed = sink.fanTime < 0 ? 0 : (float) Math.max(0, now - sink.fanTime);
        sink.fanTime = now;
        if (state.hasProperty(SinkBlock.ACTIVE) && state.getValue(SinkBlock.ACTIVE)) {
            float speed = BASE_SPEED + SPEED_PER_HEIGHT * Math.min(sink.chimneyHeight(), MAX_SPEED_HEIGHT);
            sink.fanAngle = (sink.fanAngle + elapsed * speed) % 360.0F;
        }
        return sink.fanAngle;
    }
}
