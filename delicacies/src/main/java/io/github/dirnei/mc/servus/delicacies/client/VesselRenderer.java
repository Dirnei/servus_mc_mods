package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.delicacies.cooking.PotBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlock;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselBlockEntity;
import io.github.dirnei.mc.servus.delicacies.cooking.VesselParts;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a Pot or Pan holds: the input stacks lying in a 2x2 grid, the result in the middle above them, and the
 * Pot's fluid surface rising with the fill. Items lie on the fluid in the Pot.
 */
public final class VesselRenderer<V extends VesselBlockEntity<?>> extends BufferedBlockEntityRenderer<V> {

    /** Gap between an item and the face it lies on, and the step between items that may overlap, in pixels. */
    private static final float LIFT = 0.3F;
    private static final float STEP = 0.05F;
    private static final float POT_ITEM_SCALE = 0.3F;
    private static final float PAN_ITEM_SCALE = 0.28F;

    public VesselRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(V vessel, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        Direction facing = vessel.getBlockState().getValue(VesselBlock.FACING);
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, facing);
        float[] inside = vessel instanceof PotBlockEntity ? VesselParts.POT_INSIDE : VesselParts.PAN_INSIDE;
        float floor = inside[1];
        if (vessel instanceof PotBlockEntity pot && !pot.fluid().isEmpty()) {
            floor = drawFluid(pot, inside, poseStack, buffers, light);
        }
        float scale = vessel instanceof PotBlockEntity ? POT_ITEM_SCALE : PAN_ITEM_SCALE;
        float midX = (inside[0] + inside[3]) / 2;
        float midZ = (inside[2] + inside[5]) / 2;
        float dx = (inside[3] - inside[0]) / 4;
        float dz = (inside[5] - inside[2]) / 4;
        List<ItemStack> inputs = vessel.inputs();
        int seed = (int) vessel.getBlockPos().asLong();
        for (int slot = 0; slot < inputs.size(); slot++) {
            float x = midX + (slot % 2 == 0 ? -dx : dx);
            float z = midZ + (slot < 2 ? -dz : dz);
            FlatItems.lying(poseStack, buffers.collector(), inputs.get(slot), x, floor + LIFT + slot * STEP, z, scale,
                    vessel.getLevel(), seed + slot, light);
        }
        FlatItems.lying(poseStack, buffers.collector(), vessel.output(), midX, floor + LIFT + inputs.size() * STEP + 0.5F, midZ,
                scale, vessel.getLevel(), seed + inputs.size(), light);
        poseStack.popPose();
    }

    /** The fluid surface, an up face only, from just above the bottom up to just below the rim; returns its height in pixels. */
    private static float drawFluid(PotBlockEntity pot, float[] inside, PoseStack poseStack, SubmitBuffers buffers, int light) {
        FluidStack fluid = pot.fluid();
        float share = Math.min(1, fluid.getAmount() / (float) Math.max(1, pot.capacity()));
        float height = VesselParts.POT_FLUID_LOW + (VesselParts.POT_FLUID_HIGH - VesselParts.POT_FLUID_LOW) * share;
        FluidBox surface = new FluidBox(inside[0] / 16, height / 16, inside[2] / 16, inside[3] / 16, height / 16, inside[5] / 16);
        surface.quad(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(), Direction.UP, FluidSprites.still(fluid),
                FluidSprites.tint(fluid), light);
        return height;
    }
}
