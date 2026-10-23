package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import net.minecraft.client.renderer.SubmitNodeCollector;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.data.AtlasIds;
import net.minecraft.client.Minecraft;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlock;
import io.github.dirnei.mc.servus.shuttles.garage.GarageShutter;
import io.github.dirnei.mc.servus.shuttles.garage.GarageDoorLook;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.shuttles.garage.ShuttleGarageBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesItems;
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
 * is open. A shuttle riding a lift car is drawn at the car's height. A
 * shuttle that drove in stays drawn inside until the doorway's roll-up
 * shutter is down; the shutter opens while a shuttle waits inside to leave
 * or comes close on its way in.
 */
public class ShuttleGarageRenderer extends LiveBlockEntityRenderer<ShuttleGarageBlockEntity> {

    private static final float CARGO_SCALE = 0.35F;
    /** Height of the cargo's centre above the shuttle's bottom: standing on the crate floor. */
    private static final double CARGO_Y = 4.0 / 16.0 + 0.5 * CARGO_SCALE;
    /** The wheels stand on the two rail heads, 2 pixels above the ground (or the lift car deck). */
    private static final double FLOOR = 2.0 / 16.0;
    /** The shuttle is drawn a little below block scale. */
    private static final float SHUTTLE_SCALE = 0.8F;
    /** Gap between a sight glass frame and the fluid drawn on it, in model pixels. */
    private static final float GLASS_GAP = 0.02F;
    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;
    /** The most ticks the shutter moves between two frames, so a garage seen again does not run through a whole cycle at once. */
    private static final double MAX_SHUTTER_STEP = 20;

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
        Level level = garage.getLevel();
        if (level == null) {
            return;
        }
        double now = level.getGameTime() + partialTick;
        ClientShuttles.Fleet fleet = ClientShuttles.fleet(garage.getBlockPos());
        double openness = 0;
        if (fleet != null) {
            boolean open = false;
            for (ShuttleView view : fleet.views()) {
                open |= drawShuttle(garage, view, now - fleet.receivedTick(), now, poseStack, collector);
            }
            for (ClientShuttles.Parked parked : fleet.parked()) {
                open |= drawShuttle(garage, parked.view(), now - parked.receivedTick(), now, poseStack, collector);
            }
            double ticks = Double.isNaN(fleet.shutterTime) ? 0 : Math.clamp(now - fleet.shutterTime, 0, MAX_SHUTTER_STEP);
            fleet.shutterTime = now;
            fleet.openness = GarageShutter.step(fleet.openness, open, ticks);
            if (!open && fleet.openness == 0) {
                fleet.parked().clear();
            }
            openness = fleet.openness;
        }
        drawShutter(garage, openness, poseStack, collector);
    }

    /**
     * Draws one shuttle where it is after driving the elapsed ticks since its
     * view arrived; true when it wants the shutter open.
     */
    private static boolean drawShuttle(ShuttleGarageBlockEntity garage, ShuttleView view, double elapsed, double now, PoseStack poseStack,
                                       SubmitNodeCollector collector) {
        Level level = garage.getLevel();
        BlockPos origin = garage.getBlockPos();
        double[] at = ShuttleGlide.at(view, elapsed);
        double[] pos = position(view.path(), at);
        if (view.lift().isPresent()) {
            // Riding: the car carries the shuttle, so its height comes from the car.
            ClientLifts.Lift lift = ClientLifts.lift(view.lift().get());
            if (lift != null) {
                pos[1] = lift.yAt(now);
            }
        }
        double yaw = heading(view.path(), at);
        BlockPos railPos = BlockPos.containing(pos[0], pos[1], pos[2]);
        int light = LevelRenderer.getLightCoords(level, railPos.above());

        poseStack.pushPose();
        // The model's bottom stands on the floor; NONE draws it around its centre.
        poseStack.translate(pos[0] - origin.getX() + 0.5, pos[1] - origin.getY() + FLOOR,
                pos[2] - origin.getZ() + 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw));
        poseStack.scale(SHUTTLE_SCALE, SHUTTLE_SCALE, SHUTTLE_SCALE);
        poseStack.translate(0.0, 0.5, 0.0);
        ItemStack cart = view.shuttle().isEmpty() ? new ItemStack(ServusShuttlesItems.SHUTTLE_MK1.get()) : view.shuttle();
        RenderSubmit.item(poseStack, collector, cart, ItemDisplayContext.NONE, level, 0, light);
        view.glass().ifPresent(glass -> sightGlass(glass, level, railPos, poseStack, collector, light));
        if (!view.cargo().isEmpty() && ShuttleLook.openHold(ShuttleItem.tier(cart))) {
            poseStack.translate(0.0, CARGO_Y - 0.5, 0.0);
            poseStack.scale(CARGO_SCALE, CARGO_SCALE, CARGO_SCALE);
            RenderSubmit.item(poseStack, collector, view.cargo(), ItemDisplayContext.FIXED, level, 1, light);
        }
        poseStack.popPose();
        List<BlockPos> path = view.path();
        return GarageShutter.wantsOpen(path.getFirst().equals(origin), path.size() > 1 && path.getLast().equals(origin), path.size(),
                at[0] + at[1], view.drive().top());
    }

    /**
     * The roll-up shutter: the steel slats that still hang in the doorway,
     * each drawn at its own height so its texture rolls up with it. Drawn in
     * the garage's north built space, back to the south.
     */
    private static void drawShutter(ShuttleGarageBlockEntity garage, double openness, PoseStack poseStack, SubmitNodeCollector collector) {
        float[] bottoms = GarageDoorLook.slatBottoms(openness);
        if (Float.isNaN(bottoms[0])) {
            return;
        }
        Direction facing = garage.getBlockState().getValue(ShuttleGarageBlock.FACING);
        int light = LevelRenderer.getLightCoords(garage.getLevel(), garage.getBlockPos().relative(facing.getOpposite()));
        TextureAtlasSprite steel = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(STEEL);
        float px = 1 / 16F;
        FluidBox slat = new FluidBox((GarageDoorLook.DOOR_X0 + GarageDoorLook.SLAT_INSET) * px, 0, GarageDoorLook.SLAT_Z0 * px,
                (GarageDoorLook.DOOR_X1 - GarageDoorLook.SLAT_INSET) * px, (GarageDoorLook.SLAT_HEIGHT - GarageDoorLook.SLAT_GAP) * px,
                GarageDoorLook.SLAT_Z1 * px);
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, facing);
        for (float bottom : bottoms) {
            if (Float.isNaN(bottom)) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0, bottom * px, 0);
            collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                    (pose, consumer) -> slat.draw(consumer, pose, ALL_FACES, steel, 0xFFFFFFFF, light));
            poseStack.popPose();
        }
        poseStack.popPose();
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
            if (ShuttleGlide.horizontal(path, i)) {
                return yaw(path, i);
            }
        }
        for (int i = current + 1; i < path.size() - 1; i++) {
            if (ShuttleGlide.horizontal(path, i)) {
                return yaw(path, i);
            }
        }
        return 0;
    }

    private static double yaw(List<BlockPos> path, int segment) {
        BlockPos from = path.get(segment);
        BlockPos to = path.get(segment + 1);
        // Model +z is the front: yaw 0 drives south, 90 east.
        return Math.toDegrees(Math.atan2(to.getX() - from.getX(), to.getZ() - from.getZ()));
    }
}
