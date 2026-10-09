package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the item lying on top of the Athanor; it turns slowly while the fire burns. */
public class AthanorRenderer implements BlockEntityRenderer<AthanorBlockEntity> {

    public AthanorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AthanorBlockEntity athanor, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        ItemStack stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT);
        if (stack.isEmpty() || athanor.getLevel() == null) {
            return;
        }
        boolean lit = athanor.getBlockState().hasProperty(MachineBlock.LIT) && athanor.getBlockState().getValue(MachineBlock.LIT);
        float turn = lit ? (athanor.getLevel().getGameTime() + partialTick) * 2F : 0F;
        int light = LightTexture.pack(15, 15);
        poseStack.pushPose();
        poseStack.translate(0.5, 1.02, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(turn));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, athanor.getLevel(), 0);
        poseStack.popPose();
    }
}
