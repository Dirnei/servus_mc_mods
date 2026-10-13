package io.github.dirnei.mc.servus.foundation.logistics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.dirnei.mc.servus.foundation.client.BlockSubmit;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlock;
import io.github.dirnei.mc.servus.foundation.logistics.tank.FluidTankBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

/**
 * Draws the fluid of one tank block as a box from the block bottom up to its
 * share of the capacity, with the fluid's still texture and tint. Faces
 * toward joined blocks of the same tank are skipped, so a formed tank shows
 * one continuous body of fluid. A block with nothing of its tank below it
 * also draws its floor: the block it was crafted with, squashed to a plate.
 */
public final class FluidTankRenderer implements BlockEntityRenderer<FluidTankBlockEntity, FluidTankRenderer.State> {

    /** Gap between the fluid and the glass on sides that are not joined. */
    private static final float INSET = 0.01F;
    /** Height of the floor plate; the fluid starts on top of it. */
    private static final float FLOOR = 1.0F / 16.0F;
    /** Lifts the floor off the frame's bottom faces so they do not flicker. */
    private static final float FLOOR_LIFT = 0.001F;
    /** Gap between the floor's top and the fluid's bottom, so the two surfaces never fight. */
    private static final float FLUID_GAP = 0.005F;

    /** What one frame draws of a tank block, read from the block entity on the render thread. */
    public static final class State extends BlockEntityRenderState {
        @Nullable BlockState floor;
        float fx0;
        float fx1;
        float fz0;
        float fz1;
        @Nullable TextureAtlasSprite sprite;
        int color;
        int light;
        @Nullable Box box;
        /** Faces of the fluid box to draw, one bit per direction. */
        int faces;
    }

    public FluidTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidTankBlockEntity tank, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(tank, state, partialTick, cameraPosition, breakProgress);
        state.floor = null;
        state.box = null;
        BlockState tankState = tank.getBlockState();
        if (!(tankState.getBlock() instanceof FluidTankBlock)) {
            return;
        }
        // Inset on sides that are not joined, so the floor and fluid edges stay off the glass and frame planes.
        float x0 = joined(tankState, Direction.WEST) ? 0 : INSET;
        float x1 = joined(tankState, Direction.EAST) ? 1 : 1 - INSET;
        float z0 = joined(tankState, Direction.NORTH) ? 0 : INSET;
        float z1 = joined(tankState, Direction.SOUTH) ? 1 : 1 - INSET;
        if (!joined(tankState, Direction.DOWN)) {
            state.floor = tank.floor().defaultBlockState();
            state.fx0 = x0;
            state.fx1 = x1;
            state.fz0 = z0;
            state.fz1 = z1;
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
        state.sprite = FluidSprites.still(fluid);
        state.color = FluidSprites.tint(fluid);
        int fluidLight = fluid.getFluidType().getLightLevel(fluid);
        int packedLight = state.lightCoords;
        state.light = LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(packedLight), fluidLight), LightCoordsUtil.sky(packedLight));

        boolean down = joined(tankState, Direction.DOWN);
        boolean up = joined(tankState, Direction.UP);
        float y0 = down ? 0 : FLOOR + FLOOR_LIFT + FLUID_GAP;
        float yMax = up ? 1 : 1 - INSET;
        float y1 = y0 + (yMax - y0) * fill;
        state.box = new Box(x0, y0, z0, x1, y1, z1);
        int faces = 0;
        for (Direction face : Direction.values()) {
            boolean skip = switch (face) {
                // The surface shows unless the block is full and the one above continues the fluid.
                case UP -> up && fill >= 1.0F;
                default -> joined(tankState, face);
            };
            if (!skip) {
                faces |= 1 << face.ordinal();
            }
        }
        state.faces = faces;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.floor != null) {
            poseStack.pushPose();
            poseStack.translate(state.fx0, FLOOR_LIFT, state.fz0);
            poseStack.scale(state.fx1 - state.fx0, FLOOR, state.fz1 - state.fz0);
            BlockSubmit.block(poseStack, collector, state.floor, state.lightCoords);
            poseStack.popPose();
        }
        Box box = state.box;
        TextureAtlasSprite sprite = state.sprite;
        if (box == null || sprite == null) {
            return;
        }
        int faces = state.faces;
        int color = state.color;
        int light = state.light;
        // The translucent block sheet is drawn after solid buffers, so the floor below the fluid is never hidden by it.
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            for (Direction face : Direction.values()) {
                if ((faces & (1 << face.ordinal())) != 0) {
                    quad(consumer, pose, box, face, sprite, color, light);
                }
            }
        });
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
