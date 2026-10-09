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

/** Draws the brine in the Purifier's vat: the Salt when it holds any, else the water, as high as the tank is full. */
public final class PurifierRenderer implements BlockEntityRenderer<PurifierBlockEntity> {

    /** The inside of the vat in sixteenths: above the floor, inside the walls, below the rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 2 / 16F;
    private static final float RIM = 13.5F / 16F;

    public PurifierRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PurifierBlockEntity purifier, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        FluidTank shown = purifier.salt().isEmpty() ? purifier.water() : purifier.salt();
        FluidStack fluid = shown.getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        float share = Math.min(1.0F, fluid.getAmount() / (float) Math.max(1, shown.getCapacity()));
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extensions.getStillTexture(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        float top = FLOOR + (RIM - FLOOR) * share;
        CalcinerRenderer.drawBox(buffers.getBuffer(Sheets.translucentCullBlockSheet()), poseStack.last(),
                INSIDE_MIN, FLOOR, INSIDE_MIN, INSIDE_MAX, top, INSIDE_MAX, sprite, extensions.getTintColor(fluid), packedLight);
    }
}
