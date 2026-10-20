package io.github.dirnei.mc.servus.delicacies.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.delicacies.crock.CrockParts;
import io.github.dirnei.mc.servus.delicacies.crock.FermentingCrockBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import java.util.List;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what an open Fermenting Crock holds inside its neck: the fluid surface rising with the fill, the ingredient
 * stacks lying in a 2x2 grid and the result above them. A sealed crock shows only its lid.
 */
public final class CrockRenderer extends BufferedBlockEntityRenderer<FermentingCrockBlockEntity> {

    /** Gap between an item and the face it lies on, and the step between items that may overlap, in pixels. */
    private static final float LIFT = 0.3F;
    private static final float STEP = 0.05F;
    private static final float ITEM_SCALE = 0.22F;

    public CrockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(FermentingCrockBlockEntity crock, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        if (crock.sealed()) {
            return;
        }
        float[] inside = CrockParts.INSIDE;
        float floor = inside[1];
        if (!crock.fluid().isEmpty()) {
            floor = drawFluid(crock, inside, poseStack, buffers, light);
        }
        float midX = (inside[0] + inside[3]) / 2;
        float midZ = (inside[2] + inside[5]) / 2;
        float dx = (inside[3] - inside[0]) / 4;
        float dz = (inside[5] - inside[2]) / 4;
        List<ItemStack> inputs = crock.inputs();
        int seed = (int) crock.getBlockPos().asLong();
        for (int slot = 0; slot < inputs.size(); slot++) {
            float x = midX + (slot % 2 == 0 ? -dx : dx);
            float z = midZ + (slot < 2 ? -dz : dz);
            FlatItems.lying(poseStack, buffers.collector(), inputs.get(slot), x, floor + LIFT + slot * STEP, z, ITEM_SCALE,
                    crock.getLevel(), seed + slot, light);
        }
        FlatItems.lying(poseStack, buffers.collector(), crock.output(), midX, floor + LIFT + inputs.size() * STEP + 0.5F, midZ,
                ITEM_SCALE, crock.getLevel(), seed + inputs.size(), light);
    }

    /** The fluid surface, an up face only, between the belly top and the neck's rim; returns its height in pixels. */
    private static float drawFluid(FermentingCrockBlockEntity crock, float[] inside, PoseStack poseStack, SubmitBuffers buffers, int light) {
        FluidStack fluid = crock.fluid();
        float share = Math.min(1, fluid.getAmount() / (float) Math.max(1, crock.capacity()));
        float height = CrockParts.FLUID_LOW + (CrockParts.FLUID_HIGH - CrockParts.FLUID_LOW) * share;
        FluidBox surface = new FluidBox(inside[0] / 16, height / 16, inside[2] / 16, inside[3] / 16, height / 16, inside[5] / 16);
        surface.quad(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(), Direction.UP, FluidSprites.still(fluid),
                FluidSprites.tint(fluid), light);
        return height;
    }
}
