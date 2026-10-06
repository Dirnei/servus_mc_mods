package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Draws every shuttle of this garage: a cart on the rail at the position
 * extrapolated from the last sync, with its cargo on top.
 */
public class ShuttleGarageRenderer implements BlockEntityRenderer<ShuttleGarageBlockEntity> {

    private static final float CART_SCALE = 0.6F;
    private static final float CARGO_SCALE = 0.3F;
    private static final double RAIL_TOP = 2.0 / 16.0;
    private static final double PARK_SPACING = 0.15;

    public ShuttleGarageRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(ShuttleGarageBlockEntity garage) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(ShuttleGarageBlockEntity garage) {
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void render(ShuttleGarageBlockEntity garage, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ClientShuttles.Fleet fleet = ClientShuttles.fleet(garage.getBlockPos());
        if (fleet == null || fleet.views().isEmpty() || garage.getLevel() == null) {
            return;
        }
        long now = garage.getLevel().getGameTime();
        double elapsed = (now - fleet.receivedTick()) + partialTick;
        ItemStack cart = new ItemStack(ServusStorageItems.SHUTTLE.get());
        BlockPos origin = garage.getBlockPos();
        int parked = 0;

        for (ShuttleView view : fleet.views()) {
            double[] pos = positionAfter(view, elapsed);
            double yaw = headingAfter(view, elapsed);
            double offset = 0;
            if (!view.moving()) {
                offset = parked * PARK_SPACING;
                parked++;
            }
            BlockPos railPos = BlockPos.containing(pos[0], pos[1], pos[2]);
            int light = LevelRenderer.getLightColor(garage.getLevel(), railPos.above());

            poseStack.pushPose();
            poseStack.translate(pos[0] - origin.getX() + 0.5 + offset, pos[1] - origin.getY() + RAIL_TOP + 0.5 * CART_SCALE,
                    pos[2] - origin.getZ() + 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
            poseStack.pushPose();
            poseStack.scale(CART_SCALE, CART_SCALE, CART_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(cart, ItemDisplayContext.NONE, light, packedOverlay,
                    poseStack, bufferSource, garage.getLevel(), 0);
            poseStack.popPose();
            if (!view.cargo().isEmpty()) {
                poseStack.translate(0.0, 0.5 * CART_SCALE * (3.0 / 16.0) + 0.5 * CARGO_SCALE, 0.0);
                poseStack.scale(CARGO_SCALE, CARGO_SCALE, CARGO_SCALE);
                Minecraft.getInstance().getItemRenderer().renderStatic(view.cargo(), ItemDisplayContext.FIXED, light, packedOverlay,
                        poseStack, bufferSource, garage.getLevel(), 1);
            }
            poseStack.popPose();
        }
    }

    /** Where the shuttle is after moving for the elapsed ticks since the sync. */
    private static double[] positionAfter(ShuttleView view, double elapsed) {
        List<BlockPos> path = view.path();
        int segment = view.segment();
        double progress = view.progress() + view.speed() * elapsed;
        while (segment < path.size() - 1 && progress >= 1.0) {
            progress -= 1.0;
            segment++;
        }
        if (segment >= path.size() - 1) {
            BlockPos end = path.get(path.size() - 1);
            return new double[] {end.getX(), end.getY(), end.getZ()};
        }
        BlockPos from = path.get(segment);
        BlockPos to = path.get(segment + 1);
        return new double[] {
                from.getX() + (to.getX() - from.getX()) * progress,
                from.getY() + (to.getY() - from.getY()) * progress,
                from.getZ() + (to.getZ() - from.getZ()) * progress};
    }

    /** Yaw in degrees of the segment the shuttle is on, so the cargo faces along the rail. */
    private static double headingAfter(ShuttleView view, double elapsed) {
        List<BlockPos> path = view.path();
        if (path.size() < 2) {
            return 0;
        }
        int segment = view.segment();
        double progress = view.progress() + view.speed() * elapsed;
        while (segment < path.size() - 2 && progress >= 1.0) {
            progress -= 1.0;
            segment++;
        }
        BlockPos from = path.get(Math.min(segment, path.size() - 2));
        BlockPos to = path.get(Math.min(segment + 1, path.size() - 1));
        return Math.toDegrees(Math.atan2(-(to.getX() - from.getX()), to.getZ() - from.getZ()));
    }
}
