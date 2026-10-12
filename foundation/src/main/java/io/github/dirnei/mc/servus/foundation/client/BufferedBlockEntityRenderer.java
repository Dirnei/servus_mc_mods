package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A {@link LiveBlockEntityRenderer} whose drawing code still asks a buffer
 * source for vertex consumers: it draws into {@link SubmitBuffers}, which are
 * submitted after {@link #render(BlockEntity, float, PoseStack, SubmitBuffers, int)}
 * returns, whichever way it returns.
 */
public abstract class BufferedBlockEntityRenderer<T extends BlockEntity> extends LiveBlockEntityRenderer<T> {

    @Override
    protected final void render(T blockEntity, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        SubmitBuffers buffers = new SubmitBuffers(collector);
        try {
            render(blockEntity, partialTick, poseStack, buffers, light);
        } finally {
            buffers.submit();
        }
    }

    protected abstract void render(T blockEntity, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light);
}
