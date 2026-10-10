package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.alchemy.structure.Offset;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Role;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a formed Calciner holds: the matter in the crucible, the ash heap in the Ash Pan, the Mercury in the Receiver's
 * flask, each by how full its buffer is. The Sulfur and Mercury tanks show their own level. The rest of the
 * structure is chunk baked from block models.
 */
public final class CalcinerRenderer implements BlockEntityRenderer<CalcinerBlockEntity> {

    /** A box in a cell's model space (sixteenths, front facing north) and the height its fill rises by. */
    private record Fill(Role role, Principle principle, float x0, float y0, float z0, float x1, float rise, float z1, ResourceLocation sprite) {
    }

    /** The Salt lies in the Ash Pan as ash: light grey powder, tiled like a block. */
    private static final ResourceLocation ASH = ResourceLocation.withDefaultNamespace("block/light_gray_concrete_powder");

    private static final Fill[] FILLS = {
            new Fill(Role.ASH_PAN, Principle.SALT, 4, 3, 0.5F, 12, 4, 3, ASH),
            new Fill(Role.RECEIVER, Principle.MERCURY, 4.5F, 1.25F, 4.5F, 11.5F, 7.5F, 11.5F, null),
    };

    public CalcinerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CalcinerBlockEntity calciner, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!calciner.formed()) {
            return;
        }
        Direction facing = calciner.getBlockState().getValue(MachineBlock.FACING);
        int capacity = Math.max(1, calciner.tanks().capacity());
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        for (Fill fill : FILLS) {
            int amount = calciner.tanks().amount(fill.principle());
            if (amount <= 0) {
                continue;
            }
            float share = Math.min(1.0F, amount / (float) capacity);
            FluidStack fluid = new FluidStack(PrincipleFluids.fluid(fill.principle()), amount);
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            ResourceLocation still = fill.sprite() != null ? fill.sprite() : extensions.getStillTexture(fluid);
            if (still == null) {
                continue;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
            int color = fill.sprite() != null ? 0xFFFFFFFF : extensions.getTintColor(fluid);
            Offset offset = CalcinerLayout.worldOffset(CalcinerLayout.part(fill.role()), CalcinerStructure.facing(facing));
            float[] box = rotate(fill.x0() / 16, fill.z0() / 16, fill.x1() / 16, fill.z1() / 16, facing);
            float y0 = fill.y0() / 16;
            float y1 = y0 + fill.rise() / 16 * share;
            poseStack.pushPose();
            poseStack.translate(offset.x(), offset.y(), offset.z());
            drawBox(consumer, poseStack.last(), box[0], y0, box[1], box[2], y1, box[3], sprite, color, packedLight);
            poseStack.popPose();
        }
        renderInput(calciner, facing, poseStack, buffers, packedLight);
    }

    /** The matter being calcined, standing in the crucible of the mouth. */
    private static void renderInput(CalcinerBlockEntity calciner, Direction facing, PoseStack poseStack, MultiBufferSource buffers, int light) {
        ItemStack input = calciner.shownInput();
        if (input.isEmpty()) {
            return;
        }
        float[] spot = rotate(8 / 16F, 5 / 16F, 8 / 16F, 5 / 16F, facing);
        poseStack.pushPose();
        poseStack.translate(spot[0], 10 / 16F, spot[1]);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.scale(0.4F, 0.4F, 0.4F);
        Minecraft.getInstance().getItemRenderer().renderStatic(input, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, calciner.getLevel(), (int) calciner.getBlockPos().asLong());
        poseStack.popPose();
    }

    /**
     * Turns a box drawn for a front facing north to {@code facing}, the way the blockstate turns
     * the model: clockwise seen from above, a quarter per step.
     */
    static float[] rotate(float x0, float z0, float x1, float z1, Direction facing) {
        int steps = (((int) facing.toYRot() + 180) % 360) / 90;
        for (int i = 0; i < steps; i++) {
            float nx0 = 1 - z1;
            float nx1 = 1 - z0;
            z0 = x0;
            z1 = x1;
            x0 = nx0;
            x1 = nx1;
        }
        return new float[] {x0, z0, x1, z1};
    }

    /**
     * Draws a box inside one block, the sprite tiled to the size of each face as a block model would,
     * so a low heap or a shallow liquid shows a slice of the texture instead of the whole texture squashed.
     */
    static void drawBox(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                        TextureAtlasSprite sprite, int color, int light) {
        float top = sprite.getV(1 - y1);
        float bottom = sprite.getV(1 - y0);
        quad(consumer, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0,
                sprite.getU(x0), sprite.getV(z0), sprite.getU(x1), sprite.getV(z1), color, light, Direction.UP);
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1,
                sprite.getU(z0), sprite.getV(x0), sprite.getU(z1), sprite.getV(x1), color, light, Direction.DOWN);
        quad(consumer, pose, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0,
                sprite.getU(1 - x1), top, sprite.getU(1 - x0), bottom, color, light, Direction.NORTH);
        quad(consumer, pose, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1,
                sprite.getU(x0), top, sprite.getU(x1), bottom, color, light, Direction.SOUTH);
        quad(consumer, pose, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1,
                sprite.getU(z0), top, sprite.getU(z1), bottom, color, light, Direction.WEST);
        quad(consumer, pose, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0,
                sprite.getU(1 - z1), top, sprite.getU(1 - z0), bottom, color, light, Direction.EAST);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1, int color, int light, Direction face) {
        vertex(consumer, pose, ax, ay, az, u0, v0, color, light, face);
        vertex(consumer, pose, bx, by, bz, u0, v1, color, light, face);
        vertex(consumer, pose, cx, cy, cz, u1, v1, color, light, face);
        vertex(consumer, pose, dx, dy, dz, u1, v0, color, light, face);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light, Direction face) {
        consumer.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
    }

    @Override
    public AABB getRenderBoundingBox(CalcinerBlockEntity calciner) {
        BlockPos pos = calciner.getBlockPos();
        return new AABB(pos.offset(-1, -2, -1).getCenter(), pos.offset(1, 2, 1).getCenter()).inflate(0.5);
    }
}
