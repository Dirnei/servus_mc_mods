package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Submits a whole block model from a block entity renderer, what
 * {@code BlockRenderDispatcher#renderSingleBlock} did before 26.1. Each quad is
 * drawn in its own layer; tints use the block's tint sources without a world.
 */
public final class BlockSubmit {

    private BlockSubmit() {
    }

    public static void block(PoseStack poseStack, SubmitNodeCollector collector, BlockState state, int lightCoords) {
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(42), parts);
        if (parts.isEmpty()) {
            return;
        }
        List<BlockTintSource> sources = Minecraft.getInstance().getBlockColors().getTintSources(state);
        int[] tints = new int[sources.size()];
        for (int i = 0; i < tints.length; i++) {
            tints[i] = sources.get(i).color(state);
        }
        boolean translucent = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
        collector.submitMultiLayerBlockModel(poseStack, parts, translucent, tints, lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
