package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlock;
import io.github.dirnei.mc.servus.delicacies.board.CuttingBoardBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselParts;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Draws the item to cut on the left half of the board and the pieces on the right half, both lying flat. */
public final class CuttingBoardRenderer extends BufferedBlockEntityRenderer<CuttingBoardBlockEntity> {

    /** Height the items lie at, a little above the board's top, in pixels. */
    private static final float ON_BOARD = VesselParts.BOARD[4] + 0.3F;

    public CuttingBoardRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(CuttingBoardBlockEntity board, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, board.getBlockState().getValue(CuttingBoardBlock.FACING));
        int seed = (int) board.getBlockPos().asLong();
        FlatItems.lying(poseStack, buffers.collector(), board.stack(CuttingBoardBlockEntity.SLOT_ITEM), 5.5F, ON_BOARD, 8, 0.4F,
                board.getLevel(), seed, light);
        FlatItems.lying(poseStack, buffers.collector(), board.stack(CuttingBoardBlockEntity.SLOT_PIECES), 11, ON_BOARD + 0.05F, 8, 0.3F,
                board.getLevel(), seed + 1, light);
        poseStack.popPose();
    }
}
