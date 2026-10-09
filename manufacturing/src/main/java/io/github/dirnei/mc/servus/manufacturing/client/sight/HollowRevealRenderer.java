package io.github.dirnei.mc.servus.manufacturing.client.sight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.hollow.HollowTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Draws what Elemental Sight shows of a Gnome Hollow: ore buds of the vein's yield on its faces, more and larger
 * as it ripens, and the red cap of a working gnome. Called only under the sight, by {@link RevealRenderers}.
 */
public final class HollowRevealRenderer implements RevealRenderers.RevealRenderer {

    private static final double RANGE_SQ = 32 * 32;
    /** Bud positions on the side faces and the top, inside the block's unit cube. */
    private static final float[][] BUDS = {
            {0.25F, 0.3F, -0.02F}, {0.75F, 0.7F, -0.02F},
            {1.02F, 0.35F, 0.3F}, {1.02F, 0.75F, 0.7F},
            {0.7F, 0.4F, 1.02F}, {0.3F, 0.65F, 1.02F},
            {-0.02F, 0.3F, 0.65F}, {-0.02F, 0.7F, 0.25F},
    };
    private static final ItemStack GNOME_CAP = new ItemStack(Items.RED_MUSHROOM);

    @Override
    public void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float time = (minecraft.level.getGameTime() % 400 + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 400F;
        boolean any = false;
        for (GnomeHollowBlockEntity hollow : HollowTracker.hollows()) {
            ItemStack yield = hollow.shownYield();
            BlockPos pos = hollow.getBlockPos();
            if (hollow.isRemoved() || yield.isEmpty() || pos.getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
            any = true;
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            float ripeness = hollow.shownRipeness();
            int buds = Math.max(1, Math.round(ripeness * BUDS.length));
            float scale = 0.15F + 0.25F * ripeness;
            for (int i = 0; i < buds; i++) {
                float[] bud = BUDS[i];
                poseStack.pushPose();
                poseStack.translate(bud[0], bud[1], bud[2]);
                poseStack.mulPose(Axis.YP.rotationDegrees(i * 45F + time * 360F));
                poseStack.scale(scale, scale, scale);
                minecraft.getItemRenderer().renderStatic(yield, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, poseStack, buffers, minecraft.level, i);
                poseStack.popPose();
            }
            if (hollow.shownBound()) {
                float bob = (float) Math.sin(time * Math.PI * 16) * 0.05F;
                poseStack.pushPose();
                poseStack.translate(0.5F, 1.2F + bob, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 720F));
                poseStack.scale(0.6F, 0.6F, 0.6F);
                minecraft.getItemRenderer().renderStatic(GNOME_CAP, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, poseStack, buffers, minecraft.level, 99);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
        if (any) {
            buffers.endBatch();
        }
    }
}
