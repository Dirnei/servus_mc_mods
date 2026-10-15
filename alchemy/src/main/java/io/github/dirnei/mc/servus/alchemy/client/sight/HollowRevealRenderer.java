package io.github.dirnei.mc.servus.alchemy.client.sight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.HollowTracker;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

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
    /** How much faster a gifted gnome moves; whole, so the looping time wraps without a jump. */
    private static final int GIFTED_PACE = 2;

    /** Built on first draw: item components are not bound while client setup runs. */
    private static ItemStack gnomeCap;

    @Override
    public void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        float time = (minecraft.level.getGameTime() % 400 + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 400F;
        for (GnomeHollowBlockEntity hollow : HollowTracker.hollows()) {
            ItemStack yield = hollow.shownYield();
            BlockPos pos = hollow.getBlockPos();
            if (hollow.isRemoved() || yield.isEmpty() || pos.getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
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
                RenderSubmit.item(poseStack, collector, yield, ItemDisplayContext.FIXED, minecraft.level, i, LightCoordsUtil.FULL_BRIGHT);
                poseStack.popPose();
            }
            if (hollow.shownBound()) {
                float pace = hollow.shownBoosted() ? GIFTED_PACE : 1;
                float bob = (float) Math.sin(time * pace * Math.PI * 16) * 0.05F;
                poseStack.pushPose();
                poseStack.translate(0.5F, 1.2F + bob, 0.5F);
                poseStack.mulPose(Axis.YP.rotationDegrees(time * pace * 720F));
                poseStack.scale(0.6F, 0.6F, 0.6F);
                if (gnomeCap == null) {
                    gnomeCap = new ItemStack(Items.RED_MUSHROOM);
                }
                RenderSubmit.item(poseStack, collector, gnomeCap, ItemDisplayContext.FIXED, minecraft.level, 99, LightCoordsUtil.FULL_BRIGHT);
                poseStack.popPose();
            }
            poseStack.popPose();
        }
    }
}
