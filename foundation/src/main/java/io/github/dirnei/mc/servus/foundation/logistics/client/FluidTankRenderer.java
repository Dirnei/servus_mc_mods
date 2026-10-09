package io.github.dirnei.mc.servus.foundation.logistics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws the fluid of one tank block as a box from the block bottom up to its
 * share of the capacity, with the fluid's still texture and tint. Faces
 * toward joined blocks of the same tank are skipped, so a formed tank shows
 * one continuous body of fluid. A block with nothing of its tank below it
 * also draws its floor: the block it was crafted with, squashed to a plate.
 */
public final class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {

    /** Gap between the fluid and the glass on sides that are not joined. */
    private static final float INSET = 0.01F;
    /** Height of the floor plate; the fluid starts on top of it. */
    private static final float FLOOR = 1.0F / 16.0F;
    /** Lifts the floor off the frame's bottom faces so they do not flicker. */
    private static final float FLOOR_LIFT = 0.001F;

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState tankState = tank.getBlockState();
        if (tankState.getBlock() instanceof FluidTankBlock && !joined(tankState, Direction.DOWN)) {
            poseStack.pushPose();
            poseStack.translate(0, FLOOR_LIFT, 0);
            poseStack.scale(1, FLOOR, 1);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(tank.floor().defaultBlockState(), poseStack, buffers,
                    packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poseStack.popPose();
        }
        FluidStack fluid = tank.rawShare();
        int capacity = tank.capacityPerBlock();
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        float fill = Math.min(1.0F, fluid.getAmount() / (float) capacity);
        if (fill <= 0.0F) {
            return;
        }
        BlockState state = tank.getBlockState();
        if (!(state.getBlock() instanceof FluidTankBlock)) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = extensions.getStillTexture(fluid);
        if (still == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
        int color = extensions.getTintColor(fluid);
        int fluidLight = fluid.getFluidType().getLightLevel(fluid);
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), fluidLight), LightTexture.sky(packedLight));

        boolean down = joined(state, Direction.DOWN);
        boolean up = joined(state, Direction.UP);
        float x0 = joined(state, Direction.WEST) ? 0 : INSET;
        float x1 = joined(state, Direction.EAST) ? 1 : 1 - INSET;
        float z0 = joined(state, Direction.NORTH) ? 0 : INSET;
        float z1 = joined(state, Direction.SOUTH) ? 1 : 1 - INSET;
        float y0 = down ? 0 : FLOOR + FLOOR_LIFT;
        float yMax = up ? 1 : 1 - INSET;
        float y1 = y0 + (yMax - y0) * fill;

        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();
        Box box = new Box(x0, y0, z0, x1, y1, z1);
        for (Direction face : Direction.values()) {
            boolean skip = switch (face) {
                // The surface shows unless the block is full and the one above continues the fluid.
                case UP -> up && fill >= 1.0F;
                default -> joined(state, face);
            };
            if (!skip) {
                quad(consumer, pose, box, face, sprite, color, light);
            }
        }
    }

    private static boolean joined(BlockState state, Direction side) {
        return state.getValue(FluidTankBlock.property(side));
    }

    private record Box(float x0, float y0, float z0, float x1, float y1, float z1) {
    }

    /** One face of the box, counter clockwise seen from outside, textured with the sprite scaled to the face size. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Box b, Direction face,
                             TextureAtlasSprite sprite, int color, int light) {
        switch (face) {
            case UP -> {
                float u0 = sprite.getU(b.x0()), u1 = sprite.getU(b.x1()), v0 = sprite.getV(b.z0()), v1 = sprite.getV(b.z1());
                vertex(consumer, pose, b.x0(), b.y1(), b.z0(), u0, v0, color, light, face);
                vertex(consumer, pose, b.x0(), b.y1(), b.z1(), u0, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y1(), b.z1(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y1(), b.z0(), u1, v0, color, light, face);
            }
            case DOWN -> {
                float u0 = sprite.getU(b.x0()), u1 = sprite.getU(b.x1()), v0 = sprite.getV(b.z0()), v1 = sprite.getV(b.z1());
                vertex(consumer, pose, b.x0(), b.y0(), b.z0(), u0, v0, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z0(), u1, v0, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z1(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x0(), b.y0(), b.z1(), u0, v1, color, light, face);
            }
            case NORTH -> {
                float u0 = sprite.getU(b.x0()), u1 = sprite.getU(b.x1()), v0 = sprite.getV(1 - b.y1()), v1 = sprite.getV(1 - b.y0());
                vertex(consumer, pose, b.x1(), b.y1(), b.z0(), u1, v0, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z0(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x0(), b.y0(), b.z0(), u0, v1, color, light, face);
                vertex(consumer, pose, b.x0(), b.y1(), b.z0(), u0, v0, color, light, face);
            }
            case SOUTH -> {
                float u0 = sprite.getU(b.x0()), u1 = sprite.getU(b.x1()), v0 = sprite.getV(1 - b.y1()), v1 = sprite.getV(1 - b.y0());
                vertex(consumer, pose, b.x0(), b.y1(), b.z1(), u0, v0, color, light, face);
                vertex(consumer, pose, b.x0(), b.y0(), b.z1(), u0, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z1(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y1(), b.z1(), u1, v0, color, light, face);
            }
            case WEST -> {
                float u0 = sprite.getU(b.z0()), u1 = sprite.getU(b.z1()), v0 = sprite.getV(1 - b.y1()), v1 = sprite.getV(1 - b.y0());
                vertex(consumer, pose, b.x0(), b.y1(), b.z0(), u0, v0, color, light, face);
                vertex(consumer, pose, b.x0(), b.y0(), b.z0(), u0, v1, color, light, face);
                vertex(consumer, pose, b.x0(), b.y0(), b.z1(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x0(), b.y1(), b.z1(), u1, v0, color, light, face);
            }
            case EAST -> {
                float u0 = sprite.getU(b.z0()), u1 = sprite.getU(b.z1()), v0 = sprite.getV(1 - b.y1()), v1 = sprite.getV(1 - b.y0());
                vertex(consumer, pose, b.x1(), b.y1(), b.z1(), u1, v0, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z1(), u1, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y0(), b.z0(), u0, v1, color, light, face);
                vertex(consumer, pose, b.x1(), b.y1(), b.z0(), u0, v0, color, light, face);
            }
        }
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
}
