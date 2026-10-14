package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws each principle in its Vat above a formed Coagulator, as high as its tank is full. */
public final class CoagulatorRenderer extends BufferedBlockEntityRenderer<CoagulatorBlockEntity> {

    /** The inside of a Vat in blocks: above its floor, inside its walls, below its rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 1 / 16F;
    private static final float RIM = 11.5F / 16F;

    public CoagulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(CoagulatorBlockEntity coagulator, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        if (!coagulator.formed()) {
            return;
        }
        var facing = Structures.facing(coagulator.getBlockState().getValue(MachineBlock.FACING));
        int capacity = Math.max(1, coagulator.tanks().capacity());
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentBlockSheet());
        for (int piece = 0; piece < 3; piece++) {
            CoagulatorLayout.Cell vat = CoagulatorLayout.vat(piece);
            Principle principle = CoagulatorLayout.principle(vat);
            int amount = coagulator.tanks().amount(principle);
            if (amount <= 0) {
                continue;
            }
            FluidStack fluid = new FluidStack(PrincipleFluids.fluid(principle), amount);
            TextureAtlasSprite sprite = FluidSprites.still(fluid);
            float top = FLOOR + (RIM - FLOOR) * Math.min(1.0F, amount / (float) capacity);
            Offset offset = CoagulatorLayout.worldOffset(vat, facing);
            poseStack.pushPose();
            poseStack.translate(offset.x(), offset.y(), offset.z());
            CalcinerRenderer.drawBox(consumer, poseStack.last(), INSIDE_MIN, FLOOR, INSIDE_MIN, INSIDE_MAX, top, INSIDE_MAX,
                    sprite, FluidSprites.tint(fluid), packedLight);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(CoagulatorBlockEntity coagulator) {
        BlockPos pos = coagulator.getBlockPos();
        return new AABB(pos.offset(-1, 0, -1).getCenter(), pos.offset(1, 1, 1).getCenter()).inflate(0.5);
    }
}
