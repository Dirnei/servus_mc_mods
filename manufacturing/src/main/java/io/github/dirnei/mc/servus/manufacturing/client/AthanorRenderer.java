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
import net.minecraft.world.phys.AABB;

/**
 * Draws the item lying on top of the Athanor, or the result once it is made; it turns slowly while
 * the fire burns. The pipes to the tanks are the Copper Ducts of the build.
 */
public class AthanorRenderer implements BlockEntityRenderer<AthanorBlockEntity> {

    /** Where the item rests: on the tower's iron plate when formed, on the block's top when not. */
    private static final double PLATE = 23 / 16.0 + 0.02;

    public AthanorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(AthanorBlockEntity athanor) {
        return new AABB(athanor.getBlockPos()).expandTowards(0, 1, 0);
    }

    @Override
    public void render(AthanorBlockEntity athanor, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (athanor.getLevel() == null) {
            return;
        }
        boolean formed = athanor.formed();
        ItemStack stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_INPUT);
        if (stack.isEmpty()) {
            // Once the work is done the result lies there instead.
            stack = athanor.inventory().getStackInSlot(AthanorBlockEntity.SLOT_OUTPUT);
        }
        if (stack.isEmpty()) {
            return;
        }
        boolean lit = athanor.getBlockState().hasProperty(MachineBlock.LIT) && athanor.getBlockState().getValue(MachineBlock.LIT);
        float turn = lit ? (athanor.getLevel().getGameTime() + partialTick) * 2F : 0F;
        int light = LightTexture.pack(15, 15);
        poseStack.pushPose();
        poseStack.translate(0.5, formed ? PLATE : 1.02, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(turn));
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, athanor.getLevel(), 0);
        poseStack.popPose();
    }
}
