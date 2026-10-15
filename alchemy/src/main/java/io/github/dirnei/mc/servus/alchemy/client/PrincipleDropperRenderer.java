package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.hollow.PrincipleDropperBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.PrincipleDropperTubes;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws the liquid in each of the Principle Dropper's three tubes, as high as its tank is full. */
public final class PrincipleDropperRenderer extends BufferedBlockEntityRenderer<PrincipleDropperBlockEntity> {

    /** The liquid keeps clear of the glass, so no face shares its plane. */
    private static final float INSET = 0.3F;
    private static final float FLOOR = PrincipleDropperTubes.GLASS_BOTTOM + 0.05F;
    private static final float CEILING = PrincipleDropperTubes.GLASS_TOP - 0.1F;
    /** A tube with any liquid shows at least this much, in pixels. */
    private static final float MIN_HEIGHT = 0.5F;

    public PrincipleDropperRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(PrincipleDropperBlockEntity dropper, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        int capacity = Math.max(1, dropper.tanks().capacity());
        for (Principle principle : Principle.values()) {
            FluidStack fluid = dropper.tanks().tank(principle).getFluid();
            if (fluid.isEmpty()) {
                continue;
            }
            float share = Math.min(1.0F, fluid.getAmount() / (float) capacity);
            float top = Math.max(FLOOR + MIN_HEIGHT, FLOOR + (CEILING - FLOOR) * share);
            float[] centre = PrincipleDropperTubes.CENTRES[principle.ordinal()];
            float half = PrincipleDropperTubes.RADIUS - INSET;
            CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(),
                    (centre[0] - half) / 16F, FLOOR / 16F, (centre[1] - half) / 16F,
                    (centre[0] + half) / 16F, top / 16F, (centre[1] + half) / 16F,
                    FluidSprites.still(fluid), FluidSprites.tint(fluid), light);
        }
    }
}
