package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.anchor.AnchorArea;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws the area of every Ender Anchor this player shows the range of: the
 * outer chunk borders of its square as a purple band just above the anchor's
 * height, and a purple column at each corner. Look only, like
 * {@link PlacementAssistRenderer}.
 *
 * <p>No two faces share a plane where they overlap: the bands along x run
 * through the corners and the bands along z stop at their edge, the corner
 * columns stand diagonally, and every further anchor shown is stepped by a
 * tiny amount so neighbouring squares never draw on the same plane.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class AnchorRangeRenderer {

    private static final int R = 170;
    private static final int G = 80;
    private static final int B = 255;
    private static final int A = 200;
    /** Anchors farther than this from the camera, in blocks, are not drawn. */
    private static final double RANGE = 128;
    /** Half the width of a border band, in blocks. */
    private static final float BAND = 0.06F;
    /** How far above the anchor's foot the border band lies. */
    private static final float LIFT = 0.05F;
    /** Half the width of a corner column. */
    private static final float COLUMN = 0.1F;
    private static final float COLUMN_BELOW = 16;
    private static final float COLUMN_ABOVE = 32;
    /** Step per further anchor shown, so shared borders of two squares never fight. */
    private static final float STEP = 0.005F;

    private AnchorRangeRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || AnchorRangeView.shown(level.dimension()).isEmpty()) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        List<EnderAnchorBlockEntity> anchors = new ArrayList<>();
        for (BlockPos pos : List.copyOf(AnchorRangeView.shown(level.dimension()))) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!(level.getBlockEntity(pos) instanceof EnderAnchorBlockEntity anchor)) {
                AnchorRangeView.forget(level.dimension(), pos);
                continue;
            }
            if (Vec3.atCenterOf(pos).distanceToSqr(camera.x, Vec3.atCenterOf(pos).y, camera.z) <= RANGE * RANGE) {
                anchors.add(anchor);
            }
        }
        if (anchors.isEmpty()) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.debugQuads());
        for (int i = 0; i < anchors.size(); i++) {
            EnderAnchorBlockEntity anchor = anchors.get(i);
            BlockPos pos = anchor.getBlockPos();
            ChunkPos own = ChunkPos.containing(pos);
            AnchorArea area = anchor.area();
            // Drawn relative to the square's north west corner, so small steps survive float precision far from spawn.
            int originX = area.minX(own.x()) * 16;
            int originZ = area.minZ(own.z()) * 16;
            poseStack.pushPose();
            poseStack.translate(originX - camera.x, pos.getY() - camera.y, originZ - camera.z);
            outline(consumer, poseStack.last(), area.side() * 16, i * STEP);
            poseStack.popPose();
        }
        buffers.endBatch(RenderTypes.debugQuads());
    }

    /** The outline of a square {@code size} blocks wide from the pose origin, at the anchor's foot. */
    private static void outline(VertexConsumer consumer, PoseStack.Pose pose, float size, float step) {
        float x0 = step;
        float x1 = size + step;
        float z0 = step;
        float z1 = size + step;
        float y = LIFT + step;
        // Bands along x run through the corners.
        for (float z : new float[] {z0, z1}) {
            flat(consumer, pose, x0 - BAND, z - BAND, x1 + BAND, z + BAND, y);
            upright(consumer, pose, x0 - BAND, z, x1 + BAND, z, y - BAND, y + BAND);
        }
        // Bands along z stop where the x bands begin.
        for (float x : new float[] {x0, x1}) {
            flat(consumer, pose, x - BAND, z0 + BAND, x + BAND, z1 - BAND, y);
            upright(consumer, pose, x, z0 + BAND, x, z1 - BAND, y - BAND, y + BAND);
        }
        // Columns at the corners, standing diagonally so they never lie in a band's plane.
        for (float corner : new float[] {x0, x1}) {
            // The diagonals of two squares sharing a corner would coincide; a step along x alone sets them apart.
            float x = corner + step;
            for (float z : new float[] {z0, z1}) {
                float bottom = -COLUMN_BELOW;
                float top = COLUMN_ABOVE;
                upright(consumer, pose, x - COLUMN, z - COLUMN, x + COLUMN, z + COLUMN, bottom, top);
                upright(consumer, pose, x - COLUMN, z + COLUMN, x + COLUMN, z - COLUMN, bottom, top);
            }
        }
    }

    /** A horizontal quad at height {@code y}; the debug quads pipeline does not cull, so it shows from above and below. */
    private static void flat(VertexConsumer consumer, PoseStack.Pose pose, float xa, float za, float xb, float zb, float y) {
        vertex(consumer, pose, xa, y, za);
        vertex(consumer, pose, xa, y, zb);
        vertex(consumer, pose, xb, y, zb);
        vertex(consumer, pose, xb, y, za);
    }

    /** An upright quad from ({@code xa}, {@code za}) to ({@code xb}, {@code zb}), seen from both sides. */
    private static void upright(VertexConsumer consumer, PoseStack.Pose pose, float xa, float za, float xb, float zb,
                                float bottom, float top) {
        vertex(consumer, pose, xa, bottom, za);
        vertex(consumer, pose, xb, bottom, zb);
        vertex(consumer, pose, xb, top, zb);
        vertex(consumer, pose, xa, top, za);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z) {
        consumer.addVertex(pose, x, y, z).setColor(R, G, B, A);
    }
}
