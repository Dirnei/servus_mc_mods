package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.firebox.GaugeNeedle;
import io.github.dirnei.mc.servus.delicacies.firebox.HeaterParts;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Turns the needle of a firebox thermometer: the dial is part of the block model, the needle a standalone model turned
 * from cold (left) to full heat (right) by the heat its {@link Host} reads.
 */
public final class GaugeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, GaugeRenderer.State> {

    private static final Identifier NEEDLE_ID = Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "block/gauge_needle");
    public static final StandaloneModelKey<BlockStateModelPart> NEEDLE = new StandaloneModelKey<>(NEEDLE_ID::toString);

    /**
     * Where a block entity shows a thermometer and what it reads.
     *
     * @param facing  the front of the north built model the dial belongs to
     * @param x       the dial's middle in model pixels
     * @param y       the dial's middle in model pixels
     * @param z       the dial's front face in model pixels
     * @param heat    the heat to show, 0 to 1
     */
    public record Reading(Direction facing, float x, float y, float z, float heat) {
    }

    /** Reads a block entity's thermometer, or null when it shows none. */
    @FunctionalInterface
    public interface Host<T> {
        @Nullable Reading read(T blockEntity, float partialTick);
    }

    /** The needle of one frame. */
    public static final class State extends BlockEntityRenderState {
        @Nullable Reading reading;
    }

    private final Host<T> host;

    public GaugeRenderer(Host<T> host) {
        this.host = host;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.reading = host.read(blockEntity, partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Reading reading = state.reading;
        if (reading == null) {
            return;
        }
        int light = state.lightCoords;
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, reading.facing());
        poseStack.translate((reading.x() - HeaterParts.PIVOT_X) / 16, (reading.y() - HeaterParts.PIVOT_Y) / 16, reading.z() / 16);
        ModelPoses.rotateAbout(poseStack, HeaterParts.PIVOT_X, HeaterParts.PIVOT_Y, 0, Axis.ZP, GaugeNeedle.angle(reading.heat()),
                () -> ModelKeys.submit(NEEDLE, poseStack, collector, light));
        poseStack.popPose();
    }
}
