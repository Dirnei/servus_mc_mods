package io.github.dirnei.mc.servus.alchemy.client.sight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowTracker;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Draws what Elemental Sight shows of an Ancient Hollow: the old gnome in front of its open face, the red cap of the Hollow
 * gnomes over a long white beard, swaying slowly. Once found he is drawn no more. Called only under the sight.
 */
public final class OldGnomeRevealRenderer implements RevealRenderers.RevealRenderer {

    private static final double RANGE_SQ = 32 * 32;
    /** How far in front of the open face he stands, in blocks. */
    private static final double OUT = 0.25;
    /** Cap and beard centres above the face's middle, and their sizes as item scales; they leave a gap between them. */
    private static final float CAP_Y = 0.3F;
    private static final float CAP_SCALE = 0.5F;
    private static final float BEARD_Y = -0.15F;
    private static final float BEARD_WIDE = 0.44F;
    private static final float BEARD_HIGH = 0.72F;
    private static final float BEARD_DEEP = 0.16F;

    /** Built on first draw: item components are not bound while client setup runs. */
    private static ItemStack cap;
    private static ItemStack beard;

    @Override
    public void render(SubmitCustomGeometryEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        if (cap == null) {
            cap = new ItemStack(Items.RED_MUSHROOM);
            beard = new ItemStack(Items.WHITE_WOOL);
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        float time = (minecraft.level.getGameTime() % 200 + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 200F;
        for (AncientHollowBlockEntity hollow : AncientHollowTracker.hollows()) {
            if (hollow.isRemoved() || hollow.found() || hollow.getBlockPos().getCenter().distanceToSqr(camera) > RANGE_SQ) {
                continue;
            }
            Vec3 at = hollow.inFront(OUT);
            poseStack.pushPose();
            poseStack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(-hollow.openFace().toYRot()));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * Math.PI * 2) * 4F));
            poseStack.pushPose();
            poseStack.translate(0, CAP_Y, 0);
            poseStack.scale(CAP_SCALE, CAP_SCALE, CAP_SCALE);
            RenderSubmit.item(poseStack, collector, cap, ItemDisplayContext.FIXED, minecraft.level, 0, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
            poseStack.pushPose();
            poseStack.translate(0, BEARD_Y, 0);
            poseStack.scale(BEARD_WIDE, BEARD_HIGH, BEARD_DEEP);
            RenderSubmit.item(poseStack, collector, beard, ItemDisplayContext.FIXED, minecraft.level, 1, LightCoordsUtil.FULL_BRIGHT);
            poseStack.popPose();
            poseStack.popPose();
        }
    }
}
