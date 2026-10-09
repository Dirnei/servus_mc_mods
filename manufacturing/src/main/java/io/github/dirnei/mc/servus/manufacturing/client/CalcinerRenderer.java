package io.github.dirnei.mc.servus.manufacturing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.manufacturing.alchemy.CalcinerBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout;
import io.github.dirnei.mc.servus.manufacturing.alchemy.calciner.CalcinerLayout.Offset;
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
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a formed Calciner holds: the Salt heap in the Ash Pan, the Mercury in the Receiver's
 * flask and the Sulfur crust creeping up the Hood, each by how full its tank is. The rest of the
 * structure is chunk baked from block models.
 */
public final class CalcinerRenderer implements BlockEntityRenderer<CalcinerBlockEntity> {

    /** A box in a cell's model space (sixteenths, front facing north) and the height its fill rises by. */
    private record Fill(Role role, Principle principle, float x0, float y0, float z0, float x1, float rise, float z1) {
    }

    private static final Fill[] FILLS = {
            new Fill(Role.ASH_PAN, Principle.SALT, 4, 3, 0.5F, 12, 4, 3),
            new Fill(Role.RECEIVER, Principle.MERCURY, 4.5F, 1.25F, 4.5F, 11.5F, 7.5F, 11.5F),
            new Fill(Role.HOOD, Principle.SULFUR, 4.75F, 6, 4.75F, 11.25F, 8, 11.25F),
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
            ResourceLocation still = extensions.getStillTexture(fluid);
            if (still == null) {
                continue;
            }
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
            int color = extensions.getTintColor(fluid);
            Offset offset = CalcinerLayout.worldOffset(CalcinerLayout.part(fill.role()), CalcinerStructure.facing(facing));
            float[] box = rotate(fill.x0() / 16, fill.z0() / 16, fill.x1() / 16, fill.z1() / 16, facing);
            float y0 = fill.y0() / 16;
            float y1 = y0 + fill.rise() / 16 * share;
            poseStack.pushPose();
            poseStack.translate(offset.x(), offset.y(), offset.z());
            drawBox(consumer, poseStack.last(), box[0], y0, box[1], box[2], y1, box[3], sprite, color, packedLight);
            poseStack.popPose();
        }
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

    private static void drawBox(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1,
                                TextureAtlasSprite sprite, int color, int light) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // Top and bottom
        quad(consumer, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1, color, light, Direction.UP);
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1, color, light, Direction.DOWN);
        // Sides
        quad(consumer, pose, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, u0, v0, u1, v1, color, light, Direction.NORTH);
        quad(consumer, pose, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, u0, v0, u1, v1, color, light, Direction.SOUTH);
        quad(consumer, pose, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, u0, v0, u1, v1, color, light, Direction.WEST);
        quad(consumer, pose, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, u0, v0, u1, v1, color, light, Direction.EAST);
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
