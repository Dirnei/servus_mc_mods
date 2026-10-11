package io.github.dirnei.mc.servus.storage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.storage.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.storage.registry.ServusStorageItems;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.storage.shuttle.module.ShuttleLook;
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
 * Draws every shuttle of this garage: its own item, whose model shows the
 * shuttle's parts, at full block scale on the rail at the position
 * extrapolated from the last sync, with its cargo in the crate when the hold
 * is open. A shuttle riding a lift car is drawn at the car's height.
 */
public class ShuttleGarageRenderer implements BlockEntityRenderer<ShuttleGarageBlockEntity> {

    private static final float CARGO_SCALE = 0.35F;
    /** Height of the cargo's centre above the shuttle's bottom: standing on the crate floor. */
    private static final double CARGO_Y = 4.0 / 16.0 + 0.5 * CARGO_SCALE;
    private static final double RAIL_TOP = 2.0 / 16.0;
    private static final double PARK_SPACING = 0.25;

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
        ItemStack plain = new ItemStack(ServusStorageItems.SHUTTLE_MK1.get());
        BlockPos origin = garage.getBlockPos();
        int parked = 0;

        for (ShuttleView view : fleet.views()) {
            double[] pos = positionAfter(view, elapsed);
            if (view.lift().isPresent()) {
                // Riding: the car carries the shuttle, so its height comes from the car.
                ClientLifts.Lift lift = ClientLifts.lift(view.lift().get());
                if (lift != null) {
                    pos[1] = lift.yAt(now + partialTick);
                }
            }
            double yaw = headingAfter(view, elapsed);
            double offset = 0;
            if (!view.moving()) {
                offset = parked * PARK_SPACING;
                parked++;
            }
            BlockPos railPos = BlockPos.containing(pos[0], pos[1], pos[2]);
            int light = LevelRenderer.getLightColor(garage.getLevel(), railPos.above());

            poseStack.pushPose();
            // The model's bottom stands on the rail top; NONE draws it around its centre at block scale.
            poseStack.translate(pos[0] - origin.getX() + 0.5 + offset, pos[1] - origin.getY() + RAIL_TOP + 0.5,
                    pos[2] - origin.getZ() + 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
            ItemStack cart = view.shuttle().isEmpty() ? plain : view.shuttle();
            Minecraft.getInstance().getItemRenderer().renderStatic(cart, ItemDisplayContext.NONE, light, packedOverlay,
                    poseStack, bufferSource, garage.getLevel(), 0);
            if (!view.cargo().isEmpty() && ShuttleLook.openHold(ShuttleItem.tier(cart))) {
                poseStack.translate(0.0, CARGO_Y - 0.5, 0.0);
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

    /**
     * Yaw in degrees of the segment the shuttle is on, so the cargo faces along
     * the rail. On a vertical segment it keeps the heading of the last
     * horizontal one, or takes the next one when the path starts in a shaft.
     */
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
        int current = Math.min(segment, path.size() - 2);
        for (int i = current; i >= 0; i--) {
            if (horizontal(path, i)) {
                return yaw(path, i);
            }
        }
        for (int i = current + 1; i < path.size() - 1; i++) {
            if (horizontal(path, i)) {
                return yaw(path, i);
            }
        }
        return 0;
    }

    private static boolean horizontal(List<BlockPos> path, int segment) {
        BlockPos from = path.get(segment);
        BlockPos to = path.get(segment + 1);
        return from.getX() != to.getX() || from.getZ() != to.getZ();
    }

    private static double yaw(List<BlockPos> path, int segment) {
        BlockPos from = path.get(segment);
        BlockPos to = path.get(segment + 1);
        // Model +z is the front: yaw 0 drives south, 90 east.
        return Math.toDegrees(Math.atan2(to.getX() - from.getX(), to.getZ() - from.getZ()));
    }
}
