package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EyeMotion;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EyeView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Ender Anchor's ender eye over the shut lid painted in its front
 * socket: the green lens, the iris that looks at the nearest player in front,
 * and the two lids that open while the loader holds its chunk and close for a
 * blink now and then. Wraps the {@link SideMarkerRenderer}, so the carrot and
 * the goggles still show the side markers.
 */
public class EnderAnchorRenderer implements BlockEntityRenderer<EnderAnchorBlockEntity, EnderAnchorRenderer.State> {

    private static final Identifier EYE = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "block/ender_anchor_eye");
    /** The socket on the north built front, in pixels: x and y 5..11. */
    private static final float LENS_MIN = 5;
    private static final float LENS_MAX = 11;
    private static final float IRIS = 4;
    /** Each layer sits a little further out than the one below, like the side markers. */
    private static final float LAYER = 1.0F / 256.0F;

    private final SideMarkerRenderer<EnderAnchorBlockEntity> markers;

    /** The side markers plus the eye of one frame. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State markers = new SideMarkerRenderer.State();
        float openness;
        float lid;
        float irisX;
        float irisY;
        int light;
        Direction facing = Direction.NORTH;
    }

    public EnderAnchorRenderer(BlockEntityRendererProvider.Context context) {
        markers = new SideMarkerRenderer<>(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EnderAnchorBlockEntity loader, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(loader, state, partialTick, cameraPosition, breakProgress);
        markers.extractRenderState(loader, state.markers, partialTick, cameraPosition, breakProgress);
        BlockState blockState = loader.getBlockState();
        Level level = loader.getLevel();
        BlockPos pos = loader.getBlockPos();
        state.facing = blockState.getValue(MachineBlock.FACING);
        EyeView eye = loader.eyeView();
        if (level == null) {
            state.openness = 0;
            return;
        }
        double ticks = level.getGameTime() + (double) partialTick;
        float dt = eye.advance(ticks);
        boolean active = blockState.getValue(MachineBlock.LIT);
        eye.openness = EyeMotion.openness(eye.openness, active, dt);
        float targetX = 0;
        float targetY = 0;
        if (eye.openness > 0) {
            Vec3 lens = Vec3.atCenterOf(pos).add(state.facing.getStepX() * 0.5, 0, state.facing.getStepZ() * 0.5);
            Player player = level.getNearestPlayer(lens.x, lens.y, lens.z, EyeMotion.RANGE, false);
            if (player != null) {
                float[] iris = EyeMotion.iris(player.getX() - lens.x, player.getEyeY() - lens.y, player.getZ() - lens.z,
                        quarters(state.facing));
                targetX = iris[0];
                targetY = iris[1];
            }
        }
        eye.irisX = EyeMotion.smooth(eye.irisX, targetX, dt);
        eye.irisY = EyeMotion.smooth(eye.irisY, targetY, dt);
        state.openness = eye.openness;
        state.lid = EyeMotion.lid(eye.openness, EyeMotion.blink(pos.asLong(), ticks / 20.0));
        state.irisX = eye.irisX;
        state.irisY = eye.irisY;
        state.light = LevelRenderer.getLightCoords(level, pos.relative(state.facing));
    }

    /** Quarter turns from the north built front to {@code facing}, as the blockstate rotates the model. */
    private static int quarters(Direction facing) {
        return ((int) facing.toYRot() + 180) % 360 / 90;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        markers.submit(state.markers, poseStack, collector, camera);
        // Fully shut: the lid painted in the front texture shows, nothing to draw.
        if (state.openness <= 0) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(EYE);
        float lidHeight = (LENS_MAX - LENS_MIN) / 2 * state.lid;
        float irisX = state.irisX;
        float irisY = state.irisY;
        int light = state.light;
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, state.facing);
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, consumer) -> {
            float mid = (LENS_MIN + LENS_MAX) / 2;
            // The lens: sclera u, v 0..6.
            quad(consumer, pose, sprite, LENS_MIN, LENS_MIN, LENS_MAX, LENS_MAX, 1, 0, 0, 6, 6, light);
            // The iris: u 8..12, v 0..4, moved toward the player.
            float ix = mid + irisX;
            float iy = mid + irisY;
            quad(consumer, pose, sprite, ix - IRIS / 2, iy - IRIS / 2, ix + IRIS / 2, iy + IRIS / 2, 2, 8, 0, 12, 4, light);
            if (lidHeight > 0.01F) {
                // The lids show their edge rows: the upper strip ends at v 11, the lower starts at v 12.
                float rows = Math.max(1, Math.min(3, Math.round(lidHeight)));
                quad(consumer, pose, sprite, LENS_MIN, LENS_MAX - lidHeight, LENS_MAX, LENS_MAX, 3, 0, 11 - rows, 6, 11, light);
                quad(consumer, pose, sprite, LENS_MIN, LENS_MIN, LENS_MAX, LENS_MIN + lidHeight, 3, 0, 12, 6, 12 + rows, light);
            }
        });
        poseStack.popPose();
    }

    /**
     * A quad on the north front, {@code layer} steps out of the face, from
     * {@code x0, y0} to {@code x1, y1} in pixels, showing the sprite's pixels
     * {@code u0, v0} to {@code u1, v1}. Seen from the front the viewer's left is
     * +x, so the texture's left edge lies at {@code x1}.
     */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite, float x0, float y0, float x1, float y1,
                             int layer, float u0, float v0, float u1, float v1, int light) {
        float z = -layer * LAYER;
        float a0 = sprite.getU(u0 / 16.0F);
        float a1 = sprite.getU(u1 / 16.0F);
        float b0 = sprite.getV(v0 / 16.0F);
        float b1 = sprite.getV(v1 / 16.0F);
        // Counter clockwise seen from the front.
        vertex(consumer, pose, x1 / 16.0F, y1 / 16.0F, z, a0, b0, light);
        vertex(consumer, pose, x1 / 16.0F, y0 / 16.0F, z, a0, b1, light);
        vertex(consumer, pose, x0 / 16.0F, y0 / 16.0F, z, a1, b1, light);
        vertex(consumer, pose, x0 / 16.0F, y1 / 16.0F, z, a1, b0, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0, 0, -1);
    }
}
