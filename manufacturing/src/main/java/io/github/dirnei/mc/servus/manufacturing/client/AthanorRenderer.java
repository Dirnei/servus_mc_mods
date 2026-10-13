package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Draws the item lying on top of the Athanor, or the result once it is made; it turns slowly while
 * the fire burns. The pipes to the tanks are the Copper Ducts of the build.
 */
public class AthanorRenderer extends BufferedBlockEntityRenderer<AthanorBlockEntity> {

    /** Where the item rests: on the tower's iron plate when formed, on the block's top when not. */
    private static final double PLATE = 23 / 16.0 + 0.02;

    public AthanorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(AthanorBlockEntity athanor) {
        return new AABB(athanor.getBlockPos()).expandTowards(0, 1, 0);
    }

    @Override
    protected void render(AthanorBlockEntity athanor, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        if (athanor.getLevel() == null) {
            return;
        }
        boolean formed = athanor.formed();
        ItemStack stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT);
        if (stack.isEmpty()) {
            stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT);
        }
        if (stack.isEmpty()) {
            return;
        }
        boolean lit = athanor.getBlockState().hasProperty(MachineBlock.LIT) && athanor.getBlockState().getValue(MachineBlock.LIT);
        float turn = lit ? (athanor.getLevel().getGameTime() + partialTick) * 2F : 0F;
        int light = LightCoordsUtil.pack(15, 15);
        poseStack.pushPose();
        poseStack.translate(0.5, formed ? PLATE : 1.02, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(turn));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        RenderSubmit.item(poseStack, buffers.collector(), stack, ItemDisplayContext.FIXED, athanor.getLevel(), 0, light);
        poseStack.popPose();
    }
}
