package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.athanor.AthanorLayout.Tank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Draws the item lying on top of the Athanor, or the result once it is made; it turns slowly while
 * the fire burns. A formed Athanor also gets its copper pipes: a block model reaches one block out
 * at most, and the tanks stand two out, so the pipes are drawn here, over the ring and down into
 * each tank, or into the side of a tank stacked higher.
 */
public class AthanorRenderer implements BlockEntityRenderer<AthanorBlockEntity> {

    private static final ResourceLocation COPPER = ResourceLocation.withDefaultNamespace("block/cut_copper");
    /** The pipe runs this high, in blocks above the Athanor's floor, matching the sockets of the tower model. */
    private static final float PIPE_BOTTOM = 1.2F;
    private static final float PIPE_TOP = 1.45F;
    private static final float HALF = 0.125F;
    /** Where the item rests: on the tower's iron plate when formed, on the block's top when not. */
    private static final double PLATE = 23 / 16.0 + 0.02;

    public AthanorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(AthanorBlockEntity athanor) {
        return new AABB(athanor.getBlockPos()).inflate(2, 0, 2).expandTowards(0, 1, 0);
    }

    @Override
    public void render(AthanorBlockEntity athanor, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (athanor.getLevel() == null) {
            return;
        }
        boolean formed = athanor.formed();
        if (formed) {
            pipes(athanor, poseStack, buffers);
        }
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

    /** One pipe per tank: from the tower's socket over the ring, then down into the tank's top or into a stacked tank's side. */
    private static void pipes(AthanorBlockEntity athanor, PoseStack poseStack, MultiBufferSource buffers) {
        TextureAtlasSprite copper = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(COPPER);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        BlockPos pos = athanor.getBlockPos();
        int light = LevelRenderer.getLightColor(athanor.getLevel(), pos.above());
        for (Tank tank : athanor.tankLayout()) {
            BlockPos tankPos = athanor.tankPos(tank);
            boolean stacked = athanor.getLevel().getBlockState(tankPos.above()).getBlock() instanceof FluidTankBlock;
            int ux = Integer.signum(tank.dx());
            int uz = Integer.signum(tank.dz());
            // Along the pipe: from the tower's wall (0.5 out) to the stacked tank's face (1.5) or past its middle (2 + HALF).
            float start = 0.5F;
            float end = stacked ? 1.5F : 2.0F + HALF;
            box(consumer, poseStack, along(ux, uz, start, end, PIPE_BOTTOM, PIPE_TOP), copper, light);
            if (!stacked) {
                box(consumer, poseStack, along(ux, uz, 2.0F - HALF, 2.0F + HALF, 1.0F, PIPE_BOTTOM), copper, light);
                box(consumer, poseStack, along(ux, uz, 2.0F - 1.5F * HALF, 2.0F + 1.5F * HALF, 1.0F, 1.0F + 1 / 16F, 1.5F * HALF), copper, light);
            }
        }
    }

    /** A box from {@code from} to {@code to} blocks out from the Athanor's centre in direction (ux, uz), {@code HALF} wide. */
    private static float[] along(int ux, int uz, float from, float to, float y0, float y1) {
        return along(ux, uz, from, to, y0, y1, HALF);
    }

    private static float[] along(int ux, int uz, float from, float to, float y0, float y1, float half) {
        float a = 0.5F + Math.min(from * ux, to * ux);
        float b = 0.5F + Math.max(from * ux, to * ux);
        float c = 0.5F + Math.min(from * uz, to * uz);
        float d = 0.5F + Math.max(from * uz, to * uz);
        if (ux == 0) {
            a = 0.5F - half;
            b = 0.5F + half;
        }
        if (uz == 0) {
            c = 0.5F - half;
            d = 0.5F + half;
        }
        return new float[] {a, y0, c, b, y1, d};
    }

    /** Draws a box in Athanor coordinates, split at block borders so each piece takes its texture as a block would. */
    private static void box(VertexConsumer consumer, PoseStack poseStack, float[] b, TextureAtlasSprite sprite, int light) {
        for (int x = (int) Math.floor(b[0]); x < b[3]; x++) {
            for (int y = (int) Math.floor(b[1]); y < b[4]; y++) {
                for (int z = (int) Math.floor(b[2]); z < b[5]; z++) {
                    float x0 = Math.max(b[0], x) - x;
                    float x1 = Math.min(b[3], x + 1) - x;
                    float y0 = Math.max(b[1], y) - y;
                    float y1 = Math.min(b[4], y + 1) - y;
                    float z0 = Math.max(b[2], z) - z;
                    float z1 = Math.min(b[5], z + 1) - z;
                    if (x1 <= x0 || y1 <= y0 || z1 <= z0) {
                        continue;
                    }
                    poseStack.pushPose();
                    poseStack.translate(x, y, z);
                    CalcinerRenderer.drawBox(consumer, poseStack.last(), x0, y0, z0, x1, y1, z1, sprite, 0xFFFFFFFF, light);
                    poseStack.popPose();
                }
            }
        }
    }
}
