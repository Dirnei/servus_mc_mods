package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.alchemy.structure.Offset;
import io.github.dirnei.mc.servus.alchemy.structure.Structures;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws each principle in its Vat above a formed Coagulator, as high as its tank is full, and the seed growing in the mold. */
public final class CoagulatorRenderer extends BufferedBlockEntityRenderer<CoagulatorBlockEntity> {

    /** The inside of a Vat in blocks: above its floor, inside its walls, below its rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 1 / 16F;
    private static final float RIM = 11.5F / 16F;
    /** The seed lies flat on the iron mold, whose top is at 13 px, a twentieth of a pixel above it. */
    private static final float MOLD_TOP = 13 / 16F;
    private static final float CLEARANCE = 0.05F / 16F;
    /** The item model is one pixel thick before scaling. */
    private static final float ITEM_THICKNESS = 1 / 16F;
    /** The seed's scale at rest and at the end of a cycle: 4.8 px and 9.6 px across, inside the 10 px mold. */
    private static final float SEED_REST = 0.3F;
    private static final float SEED_GROWN = 0.6F;

    public CoagulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(CoagulatorBlockEntity coagulator, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        if (!coagulator.formed()) {
            return;
        }
        renderSeed(coagulator, partialTick, poseStack, buffers, packedLight);
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

    /** The seed lying in the mold under the middle Vat's spout, growing with the running cycle. */
    private static void renderSeed(CoagulatorBlockEntity coagulator, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        ItemStack seed = coagulator.shownSeed();
        if (seed.isEmpty()) {
            return;
        }
        float scale = SEED_REST + (SEED_GROWN - SEED_REST) * coagulator.shownGrowth(partialTick);
        Direction facing = coagulator.getBlockState().getValue(MachineBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, MOLD_TOP + CLEARANCE + ITEM_THICKNESS * scale / 2, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        poseStack.scale(scale, scale, scale);
        RenderSubmit.item(poseStack, buffers.collector(), seed, ItemDisplayContext.FIXED, coagulator.getLevel(),
                (int) coagulator.getBlockPos().asLong(), light);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CoagulatorBlockEntity coagulator) {
        BlockPos pos = coagulator.getBlockPos();
        return new AABB(pos.offset(-1, 0, -1).getCenter(), pos.offset(1, 1, 1).getCenter()).inflate(0.5);
    }
}
