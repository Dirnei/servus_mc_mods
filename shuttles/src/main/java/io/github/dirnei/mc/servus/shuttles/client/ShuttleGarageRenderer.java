package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleDrive;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.TankerLook;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.client.renderer.LevelRenderer;
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
public class ShuttleGarageRenderer extends LiveBlockEntityRenderer<ShuttleGarageBlockEntity> {

    private static final float CARGO_SCALE = 0.35F;
    /** Height of the cargo's centre above the shuttle's bottom: standing on the crate floor. */
    private static final double CARGO_Y = 4.0 / 16.0 + 0.5 * CARGO_SCALE;
    /** The wheels stand on the two rail heads, 2 pixels above the ground (or the lift car deck). */
    private static final double FLOOR = 2.0 / 16.0;
    /** The shuttle is drawn a little below block scale. */
    private static final float SHUTTLE_SCALE = 0.8F;
    private static final double PARK_SPACING = 0.25 * SHUTTLE_SCALE;
    /** Gap between a sight glass frame and the fluid drawn on it, in model pixels. */
    private static final float GLASS_GAP = 0.02F;

    public ShuttleGarageRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen() {
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
    protected void render(ShuttleGarageBlockEntity garage, float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                          int packedLight) {
        ClientShuttles.Fleet fleet = ClientShuttles.fleet(garage.getBlockPos());
        if (fleet == null || fleet.views().isEmpty() || garage.getLevel() == null) {
            return;
        }
        long now = garage.getLevel().getGameTime();
        double elapsed = (now - fleet.receivedTick()) + partialTick;
        ItemStack plain = new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get());
        BlockPos origin = garage.getBlockPos();
        int parked = 0;

        for (ShuttleView view : fleet.views()) {
            double[] at = glide(view, elapsed);
            double[] pos = position(view.path(), at);
            if (view.lift().isPresent()) {
                // Riding: the car carries the shuttle, so its height comes from the car.
                ClientLifts.Lift lift = ClientLifts.lift(view.lift().get());
                if (lift != null) {
                    pos[1] = lift.yAt(now + partialTick);
                }
            }
            double yaw = heading(view.path(), at);
            double offset = 0;
            if (!view.moving()) {
                offset = parked * PARK_SPACING;
                parked++;
            }
            BlockPos railPos = BlockPos.containing(pos[0], pos[1], pos[2]);
            int light = LevelRenderer.getLightCoords(garage.getLevel(), railPos.above());

            poseStack.pushPose();
            // The model's bottom stands on the floor; NONE draws it around its centre.
            poseStack.translate(pos[0] - origin.getX() + 0.5 + offset, pos[1] - origin.getY() + FLOOR,
                    pos[2] - origin.getZ() + 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
            poseStack.scale(SHUTTLE_SCALE, SHUTTLE_SCALE, SHUTTLE_SCALE);
            poseStack.translate(0.0, 0.5, 0.0);
            ItemStack cart = view.shuttle().isEmpty() ? plain : view.shuttle();
            RenderSubmit.item(poseStack, collector, cart, ItemDisplayContext.NONE, garage.getLevel(), 0, light);
            view.glass().ifPresent(glass -> sightGlass(glass, garage.getLevel(), railPos, poseStack, collector, light));
            if (!view.cargo().isEmpty() && ShuttleLook.openHold(ShuttleItem.tier(cart))) {
                poseStack.translate(0.0, CARGO_Y - 0.5, 0.0);
                poseStack.scale(CARGO_SCALE, CARGO_SCALE, CARGO_SCALE);
                RenderSubmit.item(poseStack, collector, view.cargo(), ItemDisplayContext.FIXED, garage.getLevel(), 1, light);
            }
            poseStack.popPose();
        }
    }

    /**
     * The fluid in both sight glasses of a tanker, filled to its share, just
     * outside the windows of the Tanker model; drawn in the item's model space.
     */
    private static void sightGlass(ShuttleView.SightGlass glass, Level level, BlockPos railPos, PoseStack poseStack,
                                   SubmitNodeCollector collector, int light) {
        FluidStack fluid = glass.fluid();
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int color = level instanceof BlockAndTintGetter tinted ? FluidSprites.tint(tinted, railPos, fluid) : FluidSprites.tint(fluid);
        int glow = Math.max(LightCoordsUtil.block(light), fluid.getFluidType().getLightLevel(fluid));
        int lit = LightCoordsUtil.pack(glow, LightCoordsUtil.sky(light));
        float px = 1 / 16F;
        float top = TankerLook.GLASS_Y0 + (TankerLook.GLASS_Y1 - TankerLook.GLASS_Y0) * glass.share();
        FluidBox window = new FluidBox((TankerLook.GLASS_LEFT - GLASS_GAP) * px, TankerLook.GLASS_Y0 * px, TankerLook.GLASS_Z0 * px,
                (TankerLook.GLASS_RIGHT + GLASS_GAP) * px, top * px, TankerLook.GLASS_Z1 * px);
        poseStack.pushPose();
        // The item is drawn around its centre, so its model space starts half a block down and back.
        poseStack.translate(-0.5, -0.5, -0.5);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            window.quad(consumer, pose, Direction.WEST, sprite, color, lit);
            window.quad(consumer, pose, Direction.EAST, sprite, color, lit);
        });
        poseStack.popPose();
    }

    /** Where on its path the shuttle is after driving the elapsed ticks since the sync: {segment, progress}. */
    private static double[] glide(ShuttleView view, double elapsed) {
        List<BlockPos> path = view.path();
        ShuttleView.Drive drive = view.drive();
        return ShuttleDrive.glide(path.size(), view.segment(), view.progress(), drive.speed(), drive.top(), drive.accel(), elapsed,
                i -> !horizontal(path, i), i -> corner(path, i), drive.corner());
    }

    private static boolean corner(List<BlockPos> path, int i) {
        if (i <= 0 || i >= path.size() - 1) {
            return false;
        }
        BlockPos a = path.get(i - 1);
        BlockPos b = path.get(i);
        BlockPos c = path.get(i + 1);
        return ShuttleDrive.corner(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), c.getX(), c.getY(), c.getZ());
    }

    private static double[] position(List<BlockPos> path, double[] at) {
        int segment = (int) at[0];
        if (segment >= path.size() - 1) {
            BlockPos end = path.get(path.size() - 1);
            return new double[] {end.getX(), end.getY(), end.getZ()};
        }
        double progress = at[1];
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
    private static double heading(List<BlockPos> path, double[] at) {
        if (path.size() < 2) {
            return 0;
        }
        int current = Math.min((int) at[0], path.size() - 2);
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
