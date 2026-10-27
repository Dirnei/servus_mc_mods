package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Y0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Y1;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Z0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Z1;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlock;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws what a fluid station shows beyond its block models: the window row
 * of the upper half for its tank count, each tank's fluid behind its sight
 * glass up to the synced fill, and the side markers on both halves.
 */
public final class FluidStationRenderer implements BlockEntityRenderer<FluidStationBlockEntity, FluidStationRenderer.State> {

    private static final float PX = 1 / 16F;
    private static final List<StandaloneModelKey<BlockStateModelPart>> WINDOWS = windowKeys();

    private final SideMarkerRenderer<FluidStationBlockEntity> markers;

    /** The markers of both halves and the station, read again at submit time in the same frame. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State lower = new SideMarkerRenderer.State();
        final SideMarkerRenderer.State upper = new SideMarkerRenderer.State();
        @Nullable FluidStationBlockEntity station;
        int upperLight;
    }

    public FluidStationRenderer(BlockEntityRendererProvider.Context context) {
        markers = new SideMarkerRenderer<>(context);
    }

    private static List<StandaloneModelKey<BlockStateModelPart>> windowKeys() {
        List<StandaloneModelKey<BlockStateModelPart>> keys = new ArrayList<>();
        for (int tanks = 1; tanks <= ShuttlesStatsManager.MAX_FLUID_STATION_TANKS; tanks++) {
            Identifier id = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "block/" + FluidStationLook.windowModel(tanks));
            keys.add(new StandaloneModelKey<>(id::toString));
        }
        return List.copyOf(keys);
    }

    /** Registers the window rows for loading. */
    public static void register(ModelEvent.RegisterStandalone event) {
        WINDOWS.forEach(key -> ModelKeys.register(event, key));
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
        Level level = station.getLevel();
        state.upperLight = level == null ? state.lightCoords : LevelRenderer.getLightCoords(level, station.upperPos());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        FluidStationBlockEntity station = state.station;
        state.station = null;
        markers.submit(state.lower, poseStack, collector, camera);
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        markers.submit(state.upper, poseStack, collector, camera);
        if (station != null && station.getLevel() != null && station.getBlockState().getBlock() instanceof FluidStationBlock) {
            ModelPoses.faceFromNorth(poseStack, station.getBlockState().getValue(FluidStationBlock.FACING));
            windows(station, poseStack, collector, state.upperLight);
        }
        poseStack.popPose();
    }

    /** The window row and each tank's fluid, in the north built upper half. */
    private static void windows(FluidStationBlockEntity station, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        int tanks = Math.clamp(station.shownTanks(), 1, WINDOWS.size());
        ModelKeys.submit(WINDOWS.get(tanks - 1), poseStack, collector, light);
        Level level = station.getLevel();
        BlockPos upper = station.upperPos();
        for (int tank = 0; tank < tanks; tank++) {
            FluidStack fluid = station.shownFluid(tank);
            float fill = station.shownFill(tank);
            if (fluid.isEmpty() || fill <= 0) {
                continue;
            }
            float x0 = FluidStationLook.windowX0(tanks, tank) + FLUID_INSET;
            float x1 = FluidStationLook.windowX1(tanks, tank) - FLUID_INSET;
            float top = FLUID_Y0 + (FLUID_Y1 - FLUID_Y0) * fill;
            int faces = bit(Direction.NORTH) | bit(Direction.UP) | bit(Direction.EAST) | bit(Direction.WEST);
            FluidBox box = new FluidBox(x0 * PX, FLUID_Y0 * PX, FLUID_Z0 * PX, x1 * PX, top * PX, FLUID_Z1 * PX);
            FluidColumn.box(level, upper, upper, fluid, box, faces, poseStack, collector);
        }
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
