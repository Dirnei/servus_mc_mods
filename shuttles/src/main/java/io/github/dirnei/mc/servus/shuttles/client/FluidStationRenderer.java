package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Y0;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws what a fluid station shows beyond its block models: the tank's
 * fluid in the lower half up to the synced fill, behind the glass on all
 * four sides, and the side markers on both halves.
 */
public final class FluidStationRenderer implements BlockEntityRenderer<FluidStationBlockEntity, FluidStationRenderer.State> {

    private static final float PX = 1 / 16F;
    private static final int FLUID_FACES = bit(Direction.NORTH) | bit(Direction.SOUTH) | bit(Direction.EAST) | bit(Direction.WEST)
            | bit(Direction.UP);

    private final SideMarkerRenderer<FluidStationBlockEntity> markers;

    /** The markers of both halves and the station, read again at submit time in the same frame. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State lower = new SideMarkerRenderer.State();
        final SideMarkerRenderer.State upper = new SideMarkerRenderer.State();
        @Nullable FluidStationBlockEntity station;
    }

    public FluidStationRenderer(BlockEntityRendererProvider.Context context) {
        markers = new SideMarkerRenderer<>(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidStationBlockEntity station, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(station, state, partialTick, cameraPosition, breakProgress);
        markers.extractRenderState(station, state.lower, partialTick, cameraPosition, breakProgress);
        markers.extractRenderState(station, state.upper, partialTick, cameraPosition, breakProgress);
        state.lower.hide(Direction.UP);
        state.upper.hide(Direction.DOWN);
        state.station = station;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        FluidStationBlockEntity station = state.station;
        state.station = null;
        markers.submit(state.lower, poseStack, collector, camera);
        if (station != null && station.getLevel() != null) {
            fluid(station, poseStack, collector);
        }
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        markers.submit(state.upper, poseStack, collector, camera);
        poseStack.popPose();
    }

    /** The tank's fluid, inside the glass of the lower half. */
    private static void fluid(FluidStationBlockEntity station, PoseStack poseStack, SubmitNodeCollector collector) {
        FluidStack fluid = station.shownFluid();
        float fill = station.shownFill();
        if (fluid.isEmpty() || fill <= 0) {
            return;
        }
        float in = FLUID_INSET * PX;
        float out = (16 - FLUID_INSET) * PX;
        FluidBox box = new FluidBox(in, FLUID_Y0 * PX, in, out, FluidStationLook.fluidTop(fill) * PX, out);
        BlockPos pos = station.getBlockPos();
        FluidColumn.box(station.getLevel(), pos, pos, fluid, box, FLUID_FACES, poseStack, collector);
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    /** Both halves. */
    @Override
    public AABB getRenderBoundingBox(FluidStationBlockEntity station) {
        BlockPos pos = station.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1).inflate(PX);
    }
}
