package io.github.dirnei.mc.servus.alchemy.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.alchemy.prelum.PrelumBlock;
import io.github.dirnei.mc.servus.alchemy.prelum.PrelumBlockEntity;
import io.github.dirnei.mc.servus.foundation.client.BufferedBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.SubmitBuffers;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Draws the moving parts of the Prelum in steel: the press plate, the screw and the bar with its two knobs on top. While
 * the Prelum presses, the bar turns once as the plate goes down onto the bed and turns back as it rises; idle, the plate
 * rests at the top.
 */
public final class PrelumRenderer extends BufferedBlockEntityRenderer<PrelumBlockEntity> {

    private static final float PX = 1 / 16F;
    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("servus_foundation", "block/electric_pump_steel");
    private static final int WHITE = 0xFFFFFFFF;
    /** One press cycle in ticks, and the part of it the plate takes to go down. */
    private static final float CYCLE = 80;
    private static final float DOWN_TICKS = 60;
    /**
     * Half the height of the moving parts in pixels, from the plate's bottom to the knobs' top. They are drawn around their
     * middle, so the box drawer's texture coordinates, taken from a place in the unit cube, stay on the sprite.
     */
    private static final float MIDDLE = 4.45F;

    public PrelumRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(PrelumBlockEntity prelum, float partialTick, PoseStack poseStack, SubmitBuffers buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        float down = 0;
        if (minecraft.level != null && prelum.getBlockState().getValue(MachineBlock.LIT)) {
            float tick = (minecraft.level.getGameTime() % (long) CYCLE) + partialTick;
            down = tick < DOWN_TICKS ? tick / DOWN_TICKS : 1 - (tick - DOWN_TICKS) / (CYCLE - DOWN_TICKS);
            down = Math.min(1, Math.max(0, down));
        }
        float plate = PrelumBlock.PLATE_REST - (PrelumBlock.PLATE_REST - PrelumBlock.PLATE_DOWN) * down;
        TextureAtlasSprite steel = minecraft.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(STEEL);
        VertexConsumer consumer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        poseStack.pushPose();
        poseStack.translate(0.5F, (plate + MIDDLE) * PX, 0.5F);
        VasRenderer.pixelBox(consumer, poseStack, -3.5F, -MIDDLE, -3.5F, 3.5F, 1.5F - MIDDLE, 3.5F, steel, WHITE, light);
        poseStack.mulPose(Axis.YP.rotationDegrees(360 * down));
        VasRenderer.pixelBox(consumer, poseStack, -1.25F, 1.5F - MIDDLE, -1.25F, 1.25F, 7.5F - MIDDLE, 1.25F, steel, WHITE, light);
        VasRenderer.pixelBox(consumer, poseStack, -5, 7.5F - MIDDLE, -0.5F, 5, 8.5F - MIDDLE, 0.5F, steel, WHITE, light);
        VasRenderer.pixelBox(consumer, poseStack, -6.5F, 7.5F - MIDDLE, -0.9F, -5, 8.9F - MIDDLE, 0.9F, steel, WHITE, light);
        VasRenderer.pixelBox(consumer, poseStack, 5, 7.5F - MIDDLE, -0.9F, 6.5F, 8.9F - MIDDLE, 0.9F, steel, WHITE, light);
        poseStack.popPose();
    }
}
