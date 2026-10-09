package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CoagulatorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.coagulator.CoagulatorLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Structures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws each principle in its Vat above a formed Coagulator, as high as its tank is full. */
public final class CoagulatorRenderer implements BlockEntityRenderer<CoagulatorBlockEntity> {

    /** The inside of a Vat in blocks: above its floor, inside its walls, below its rim. */
    private static final float INSIDE_MIN = 3 / 16F;
    private static final float INSIDE_MAX = 13 / 16F;
    private static final float FLOOR = 1 / 16F;
    private static final float RIM = 11.5F / 16F;

    public CoagulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CoagulatorBlockEntity coagulator, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!coagulator.formed()) {
            return;
        }
        var facing = Structures.facing(coagulator.getBlockState().getValue(MachineBlock.FACING));
        int capacity = Math.max(1, coagulator.tanks().capacity());
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        for (int piece = 0; piece < 3; piece++) {
            CoagulatorLayout.Cell vat = CoagulatorLayout.vat(piece);
            Principle principle = CoagulatorLayout.principle(vat);
            int amount = coagulator.tanks().amount(principle);
            if (amount <= 0) {
                continue;
            }
            FluidStack fluid = new FluidStack(PrincipleFluids.fluid(principle), amount);
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            ResourceLocation still = extensions.getStillTexture(fluid);
            if (still == null) {
                continue;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
            float top = FLOOR + (RIM - FLOOR) * Math.min(1.0F, amount / (float) capacity);
            Offset offset = CoagulatorLayout.worldOffset(vat, facing);
            poseStack.pushPose();
            poseStack.translate(offset.x(), offset.y(), offset.z());
            CalcinerRenderer.drawBox(consumer, poseStack.last(), INSIDE_MIN, FLOOR, INSIDE_MIN, INSIDE_MAX, top, INSIDE_MAX,
                    sprite, extensions.getTintColor(fluid), packedLight);
            poseStack.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(CoagulatorBlockEntity coagulator) {
        BlockPos pos = coagulator.getBlockPos();
        return new AABB(pos.offset(-1, 0, -1).getCenter(), pos.offset(1, 1, 1).getCenter()).inflate(0.5);
    }
}
