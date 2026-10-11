package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A block entity renderer that keeps the block entity in its render state and
 * draws from it at submit time, the shape of a 1.21.1 renderer. Both steps run
 * on the render thread in the same frame, so reading the block entity in
 * {@link #render} is what the old {@code render} method did.
 */
public abstract class LiveBlockEntityRenderer<T extends BlockEntity>
        implements BlockEntityRenderer<T, LiveBlockEntityRenderer.State<T>> {

    public static final class State<T> extends BlockEntityRenderState {
        @Nullable T blockEntity;
        float partialTick;
    }

    @Override
    public State<T> createRenderState() {
        return new State<>();
    }

    @Override
    public void extractRenderState(T blockEntity, State<T> state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        state.blockEntity = blockEntity;
        state.partialTick = partialTick;
    }

    @Override
    public void submit(State<T> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        T blockEntity = state.blockEntity;
        state.blockEntity = null;
        if (blockEntity != null) {
            render(blockEntity, state.partialTick, poseStack, collector, state.lightCoords);
        }
    }

    /** Draws the block entity; {@code light} is the packed light at its position. */
    protected abstract void render(T blockEntity, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light);
}
