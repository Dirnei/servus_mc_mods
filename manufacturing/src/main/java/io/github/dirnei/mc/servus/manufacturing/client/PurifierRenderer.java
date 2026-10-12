package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import net.minecraft.data.AtlasIds;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;

/** Draws the Purifier: the Ash still to wash on the Vat's floor with the water over it, the Salt in the Catch Basin and the Caput Mortuum in the Residue Crock. */
public final class PurifierRenderer extends BufferedBlockEntityRenderer<PurifierBlockEntity> {

    /** The inside of the vat in sixteenths: above the floor, inside the walls, below the rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 2 / 16F;
    private static final float RIM = 13.5F / 16F;
    /** The inside of the Catch Basin, in blocks. */
    private static final float BASIN_FLOOR = 2 / 16F;
    private static final float BASIN_RIM = 9.5F / 16F;
    /** The inside of the Residue Crock, in blocks. */
    private static final float CROCK_MIN = 4 / 16F;
    private static final float CROCK_MAX = 12 / 16F;
    private static final float CROCK_FLOOR = 2 / 16F;
    private static final float CROCK_TOP = 9.5F / 16F;
    /** The Caput Mortuum is a dark violet brown powder. */
    private static final Identifier RESIDUE = Identifier.withDefaultNamespace("block/brown_concrete_powder");

    public PurifierRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The Ash waiting in the vat lies on its floor as grey powder. */
    private static final Identifier ASH = Identifier.withDefaultNamespace("block/light_gray_concrete_powder");
    /** The ash layer grows from one sixteenth to this height with a full stack. */
    private static final float ASH_MAX = 3 / 16F;

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PurifierBlockEntity purifier) {
        return new net.minecraft.world.phys.AABB(purifier.getBlockPos()).inflate(1, 0, 1).expandTowards(0, -1, 0);
    }

    @Override
    protected void render(PurifierBlockEntity purifier, float partialTick, PoseStack poseStack, SubmitBuffers buffers,
                          int packedLight) {
        int packedOverlay = OverlayTexture.NO_OVERLAY;
        float floor = FLOOR;
        int items = purifier.shownItems();
        if (items > 0) {
            TextureAtlasSprite ash = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(ASH);
            float height = 1 / 16F + (ASH_MAX - 1 / 16F) * Math.min(1.0F, items / 64F);
            CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(),
                    INSIDE_MIN, FLOOR, INSIDE_MIN, INSIDE_MAX, FLOOR + height, INSIDE_MAX, ash, 0xFFFFFFFF, packedLight);
            floor = FLOOR + height;
        }
        drawLiquid(purifier.water(), floor, FLOOR, RIM, poseStack, buffers, packedLight);
        if (!purifier.formed() || purifier.getLevel() == null) {
            return;
        }
        // The Salt runs down into the Catch Basin, the Caput Mortuum into the Residue Crock.
        BlockPos vat = purifier.getBlockPos();
        BlockPos basin = PurifierStructure.partPos(purifier.getLevel(), vat, PurifierLayout.Role.BASIN).subtract(vat);
        poseStack.pushPose();
        poseStack.translate(basin.getX(), basin.getY(), basin.getZ());
        drawLiquid(purifier.salt(), BASIN_FLOOR, BASIN_FLOOR, BASIN_RIM, poseStack, buffers, packedLight);
        poseStack.popPose();
        int residue = purifier.shownResidue();
        if (residue > 0) {
            BlockPos crock = PurifierStructure.partPos(purifier.getLevel(), vat, PurifierLayout.Role.CROCK).subtract(vat);
            TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(RESIDUE);
            float top = CROCK_FLOOR + 1 / 16F + (CROCK_TOP - CROCK_FLOOR - 1 / 16F) * Math.min(1.0F, residue / 64F);
            poseStack.pushPose();
            poseStack.translate(crock.getX(), crock.getY(), crock.getZ());
            CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(),
                    CROCK_MIN, CROCK_FLOOR, CROCK_MIN, CROCK_MAX, top, CROCK_MAX, sprite, 0xFFFFFFFF, packedLight);
            poseStack.popPose();
        }
    }

    /** One liquid as high as its tank is full, between {@code bottom} and {@code rim}, never thinner than half a pixel above {@code floor}. */
    private static void drawLiquid(FluidSlot tank, float floor, float bottom, float rim, PoseStack poseStack, SubmitBuffers buffers, int light) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        float share = Math.min(1.0F, fluid.getAmount() / (float) Math.max(1, tank.getCapacity()));
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        float top = Math.max(floor + 0.5F / 16F, bottom + (rim - bottom) * share);
        CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentBlockSheet()), poseStack.last(),
                INSIDE_MIN, floor, INSIDE_MIN, INSIDE_MAX, top, INSIDE_MAX, sprite, FluidSprites.tint(fluid), light);
    }
}
