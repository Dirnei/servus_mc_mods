package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.block.ConnectableBlock;
import io.github.dirnei.mc.servus.foundation.item.GogglesItem;
import io.github.dirnei.mc.servus.foundation.data.ClientDuctLoads;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctTracker;
import io.github.dirnei.mc.servus.foundation.logistics.duct.Line;
import io.github.dirnei.mc.servus.foundation.logistics.duct.UtilityDuctBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * While the player reveals hidden things: draws facaded ducts as translucent
 * boxes through walls, and the centre of every data duct in the colour of its
 * load over the last second, from idle green to full red.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class DuctOverlayRenderer {

    private static final double RANGE_SQ = 32.0 * 32.0;
    private static final float LO = 4.0F / 16.0F;
    private static final float HI = 12.0F / 16.0F;
    private static final int R = 255;
    private static final int G = 170;
    private static final int B = 60;
    private static final int A = 110;
    private static final float LOAD_LO = 3.5F / 16.0F;
    private static final float LOAD_HI = 12.5F / 16.0F;
    /** Load colours by level: idle, light, half, busy, full. */
    private static final int[] LOAD_COLOURS = {0x3CC85A, 0xA0DC3C, 0xF0DC3C, 0xFF9628, 0xF03228};

    private DuctOverlayRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !GogglesItem.reveals(minecraft.player)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(ServusRenderTypes.OVERLAY);
        boolean any = false;
        for (UtilityDuctBlockEntity duct : DuctTracker.ducts()) {
            if (duct.isRemoved()) {
                continue;
            }
            BlockPos pos = duct.getBlockPos();
            if (pos.getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
            if (duct.has(Line.DATA)) {
                any = true;
                int colour = LOAD_COLOURS[Math.min(LOAD_COLOURS.length - 1, ClientDuctLoads.level(pos))];
                poseStack.pushPose();
                poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                box(consumer, poseStack.last(), LOAD_LO, LOAD_LO, LOAD_LO, LOAD_HI, LOAD_HI, LOAD_HI,
                        (colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF);
                poseStack.popPose();
            }
            if (!duct.hasPanels()) {
                continue;
            }
            any = true;
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            PoseStack.Pose pose = poseStack.last();
            box(consumer, pose, LO, LO, LO, HI, HI, HI);
            BlockState state = duct.getBlockState();
            for (Direction direction : Direction.values()) {
                if (state.hasProperty(ConnectableBlock.property(direction)) && state.getValue(ConnectableBlock.property(direction))) {
                    arm(consumer, pose, direction);
                }
            }
            poseStack.popPose();
        }
        if (any) {
            buffers.endBatch(ServusRenderTypes.OVERLAY);
        }
    }

    private static void arm(VertexConsumer consumer, PoseStack.Pose pose, Direction direction) {
        switch (direction) {
            case NORTH -> box(consumer, pose, LO, LO, 0.0F, HI, HI, LO);
            case SOUTH -> box(consumer, pose, LO, LO, HI, HI, HI, 1.0F);
            case WEST -> box(consumer, pose, 0.0F, LO, LO, LO, HI, HI);
            case EAST -> box(consumer, pose, HI, LO, LO, 1.0F, HI, HI);
            case DOWN -> box(consumer, pose, LO, 0.0F, LO, HI, LO, HI);
            case UP -> box(consumer, pose, LO, HI, LO, HI, 1.0F, HI);
        }
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1) {
        box(consumer, pose, x0, y0, z0, x1, y1, z1, R, G, B);
    }

    private static void box(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                            int r, int g, int b) {
        quad(consumer, pose, r, g, b, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
        quad(consumer, pose, r, g, b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(consumer, pose, r, g, b, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(consumer, pose, r, g, b, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
        quad(consumer, pose, r, g, b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(consumer, pose, r, g, b, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int r, int g, int b,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        consumer.addVertex(pose, ax, ay, az).setColor(r, g, b, A);
        consumer.addVertex(pose, bx, by, bz).setColor(r, g, b, A);
        consumer.addVertex(pose, cx, cy, cz).setColor(r, g, b, A);
        consumer.addVertex(pose, dx, dy, dz).setColor(r, g, b, A);
    }
}
