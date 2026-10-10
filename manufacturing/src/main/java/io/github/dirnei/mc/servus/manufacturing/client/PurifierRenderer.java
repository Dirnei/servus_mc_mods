package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.manufacturing.alchemy.purifier.PurifierBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Draws the Purifier: the Ash still to wash on the Vat's floor with the water over it, and the Salt in the Catch Basin below. */
public final class PurifierRenderer implements BlockEntityRenderer<PurifierBlockEntity> {

    /** The inside of the vat in sixteenths: above the floor, inside the walls, below the rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 2 / 16F;
    private static final float RIM = 13.5F / 16F;
    /** The inside of the Catch Basin below, in blocks. */
    private static final float BASIN_FLOOR = 2 / 16F;
    private static final float BASIN_RIM = 9.5F / 16F;

    public PurifierRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The Ash waiting in the vat lies on its floor as grey powder. */
    private static final ResourceLocation ASH = ResourceLocation.withDefaultNamespace("block/light_gray_concrete_powder");
    /** The ash layer grows from one sixteenth to this height with a full stack. */
    private static final float ASH_MAX = 3 / 16F;

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(PurifierBlockEntity purifier) {
        return new net.minecraft.world.phys.AABB(purifier.getBlockPos()).expandTowards(0, -1, 0);
    }

    @Override
    public void render(PurifierBlockEntity purifier, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        float floor = FLOOR;
        int items = purifier.shownItems();
        if (items > 0) {
            TextureAtlasSprite ash = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ASH);
            float height = 1 / 16F + (ASH_MAX - 1 / 16F) * Math.min(1.0F, items / 64F);
            CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack.last(),
                    INSIDE_MIN, FLOOR, INSIDE_MIN, INSIDE_MAX, FLOOR + height, INSIDE_MAX, ash, 0xFFFFFFFF, packedLight);
            floor = FLOOR + height;
        }
        drawLiquid(purifier.water(), floor, FLOOR, RIM, poseStack, buffers, packedLight);
        // The Salt drips through into the Catch Basin below.
        if (purifier.formed()) {
            poseStack.pushPose();
            poseStack.translate(0, -1, 0);
            drawLiquid(purifier.salt(), BASIN_FLOOR, BASIN_FLOOR, BASIN_RIM, poseStack, buffers, packedLight);
            poseStack.popPose();
        }
    }

    /** One liquid as high as its tank is full, between {@code bottom} and {@code rim}, never thinner than half a pixel above {@code floor}. */
    private static void drawLiquid(FluidTank tank, float floor, float bottom, float rim, PoseStack poseStack, MultiBufferSource buffers, int light) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        float share = Math.min(1.0F, fluid.getAmount() / (float) Math.max(1, tank.getCapacity()));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extensions.getStillTexture(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        float top = Math.max(floor + 0.5F / 16F, bottom + (rim - bottom) * share);
        CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack.last(),
                INSIDE_MIN, floor, INSIDE_MIN, INSIDE_MAX, top, INSIDE_MAX, sprite, extensions.getTintColor(fluid), light);
    }
}
