package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook.FLUID_Y0;
import static io.github.dirnei.mc.servus.shuttles.fluidstation.StationArmLook.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.SideMarkerRenderer;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidStationLook;
import io.github.dirnei.mc.servus.shuttles.fluidstation.StationArmLook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws what a fluid station shows beyond its block models: the tank's
 * fluid in the lower half up to the synced fill, behind the glass on all
 * four sides, the side markers on both halves, and the swing hose arm on
 * the front, eased from the synced start and length of a tanker's stop.
 */
public final class FluidStationRenderer implements BlockEntityRenderer<FluidStationBlockEntity, FluidStationRenderer.State> {

    private static final float PX = 1 / 16F;
    private static final int FLUID_FACES = bit(Direction.NORTH) | bit(Direction.SOUTH) | bit(Direction.EAST) | bit(Direction.WEST)
            | bit(Direction.UP);
    private static final int ALL_FACES = (1 << Direction.values().length) - 1;
    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final Identifier COPPER = Identifier.withDefaultNamespace("block/copper_block");
    private static final Identifier HOSE = Identifier.fromNamespaceAndPath("servus_shuttles", "block/heavy_rack_iron");

    private final SideMarkerRenderer<FluidStationBlockEntity> markers;

    /** The markers of both halves, the station and the arm's pose, read again at submit time in the same frame. */
    public static final class State extends BlockEntityRenderState {
        final SideMarkerRenderer.State lower = new SideMarkerRenderer.State();
        final SideMarkerRenderer.State upper = new SideMarkerRenderer.State();
        @Nullable FluidStationBlockEntity station;
        Direction facing = Direction.NORTH;
        float swing;
        float drop;
        int armLight;
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
        state.facing = station.facing();
        float elapsed = station.armElapsed(partialTick);
        state.swing = StationArmLook.swing(station.armTicks(), elapsed);
        state.drop = StationArmLook.lower(station.armTicks(), elapsed);
        state.armLight = station.getLevel() == null ? state.lightCoords
                : LevelRenderer.getLightCoords(station.getLevel(), station.upperPos().relative(state.facing));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        FluidStationBlockEntity station = state.station;
        state.station = null;
        markers.submit(state.lower, poseStack, collector, camera);
        if (station != null && station.getLevel() != null) {
            fluid(station, poseStack, collector);
        }
        arm(state, poseStack, collector);
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

    /**
     * The swing hose arm on the station's front, drawn as for a station
     * facing north and turned by the facing: the copper stub and swivel,
     * then, turned about the swivel, the steel boom with the dark hose and
     * the copper head below it, the hose let out while the boom lies level.
     * Out of the front is model z below 0.
     */
    private static void arm(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        TextureAtlasSprite copper = atlas.getSprite(COPPER);
        int light = state.armLight;
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180 - state.facing.toYRot()));
        poseStack.translate(-0.5, 0, -0.5);
        part(CENTRE_X - STUB_HALF, STUB_Y0, -PIVOT_OUT - STUB_HALF, CENTRE_X + STUB_HALF, STUB_Y1, -PIVOT_OUT + STUB_HALF, copper,
                poseStack, collector, light);
        part(CENTRE_X - SWIVEL_HALF_X, PIVOT_Y - SWIVEL_HALF, -PIVOT_OUT - SWIVEL_HALF, CENTRE_X + SWIVEL_HALF_X, PIVOT_Y + SWIVEL_HALF,
                -PIVOT_OUT + SWIVEL_HALF, copper, poseStack, collector, light);
        poseStack.translate(0, PIVOT_Y * PX, -PIVOT_OUT * PX);
        poseStack.mulPose(Axis.XP.rotationDegrees(StationArmLook.angle(state.swing)));
        part(CENTRE_X - BOOM_HALF, -BOOM_HALF, -BOOM_LENGTH, CENTRE_X + BOOM_HALF, BOOM_HALF, BOOM_BACK, atlas.getSprite(STEEL),
                poseStack, collector, light);
        float headTop = StationArmLook.headTop(state.drop * StationArmLook.hoseDrop());
        part(CENTRE_X - HOSE_HALF, headTop - HOSE_INTO_HEAD, -HOSE_AT - HOSE_HALF, CENTRE_X + HOSE_HALF, HOSE_TOP, -HOSE_AT + HOSE_HALF,
                atlas.getSprite(HOSE), poseStack, collector, light);
        part(CENTRE_X - HEAD_HALF, headTop - HEAD_HEIGHT, -HOSE_AT - HEAD_HALF, CENTRE_X + HEAD_HALF, headTop, -HOSE_AT + HEAD_HALF, copper,
                poseStack, collector, light);
        poseStack.popPose();
    }

    /** One box of the arm, in pixels of the current pose, drawn from its own corner so every face takes the sprite's corner. */
    private static void part(float x0, float y0, float z0, float x1, float y1, float z1, TextureAtlasSprite sprite, PoseStack poseStack,
                             SubmitNodeCollector collector, int light) {
        FluidBox box = new FluidBox(0, 0, 0, (x1 - x0) * PX, (y1 - y0) * PX, (z1 - z0) * PX);
        poseStack.pushPose();
        poseStack.translate(x0 * PX, y0 * PX, z0 * PX);
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
                (pose, consumer) -> box.draw(consumer, pose, ALL_FACES, sprite, 0xFFFFFFFF, light));
        poseStack.popPose();
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }

    /** Both halves and the arm reaching over the rail in front. */
    @Override
    public AABB getRenderBoundingBox(FluidStationBlockEntity station) {
        BlockPos pos = station.getBlockPos();
        Direction facing = station.facing();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1)
                .expandTowards(facing.getStepX() * 0.75, 0, facing.getStepZ() * 0.75).inflate(PX);
    }
}
