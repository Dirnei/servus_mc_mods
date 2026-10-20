package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.CAP_BOTTOM;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.FLUID_GAP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.FLUID_INSET;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_OUT;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_X1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLATE_Y1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.PLINTH_TOP;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_U0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_U1;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_V0;
import static io.github.dirnei.mc.servus.shuttles.gantry.GantryLook.WINDOW_V1;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlock;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidGantryBlockEntity;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what the Fluid Gantry shows beyond its block models, from its master
 * (the left leg): the fluid of each tank as a column inside its leg's glass,
 * from the plinth up to the synced fill, with the fluid's still texture and
 * its tint at the gantry; and on both lock plates of a locked tank the locked
 * fluid's texture over the striped Any window. Everything is laid out for a
 * rail running north to south and turned a quarter for one running east to west.
 */
public final class FluidGantryRenderer extends LiveBlockEntityRenderer<FluidGantryBlockEntity> {

    /** Gap between a lock plate's face and the fluid drawn on it. */
    private static final float PLATE_GAP = 0.01F;
    private static final float PX = 1 / 16F;
    private static final int UP = 1 << Direction.UP.ordinal();
    private static final int DOWN = 1 << Direction.DOWN.ordinal();
    private static final int SIDES_AND_DOWN = 0b111111 & ~UP;
    private static final int ALL = 0b111111;

    public FluidGantryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(FluidGantryBlockEntity gantry, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Level level = gantry.getLevel();
        BlockState state = gantry.getBlockState();
        if (level == null || !(state.getBlock() instanceof FluidGantryBlock)) {
            return;
        }
        BlockPos pos = gantry.getBlockPos();
        poseStack.pushPose();
        if (state.getValue(FluidGantryBlock.AXIS) == Direction.Axis.X) {
            // The models turn a quarter clockwise seen from above for a rail running east to west.
            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            poseStack.translate(-0.5, 0, -0.5);
        }
        for (int tank = 0; tank < 2; tank++) {
            BlockPos leg = FluidGantryBlock.partPos(pos, state, tank == 0 ? GantryPart.LEFT_LOW : GantryPart.RIGHT_LOW);
            poseStack.pushPose();
            poseStack.translate(tank * 2, 0, 0);
            column(level, leg, gantry.shownFluid(tank), gantry.shownFill(tank), poseStack, collector);
            Fluid lock = gantry.lock(tank);
            if (lock != null) {
                plates(level, leg.above(), new FluidStack(lock, 1000), poseStack, collector);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The fluid of one tank: a box in the lower leg block and, once it reaches higher, one in the upper block. */
    private static void column(Level level, BlockPos leg, FluidStack fluid, float fill, PoseStack poseStack, SubmitNodeCollector collector) {
        if (fluid.isEmpty() || fill <= 0) {
            return;
        }
        TextureAtlasSprite sprite = FluidSprites.still(fluid);
        int color = level instanceof BlockAndTintGetter tinted ? FluidSprites.tint(tinted, leg, fluid) : FluidSprites.tint(fluid);
        int light = glowing(LevelRenderer.getLightCoords(level, leg.above()), fluid);
        float in = FLUID_INSET * PX;
        float bottom = (PLINTH_TOP + FLUID_GAP) * PX;
        float top = bottom + ((1 + (CAP_BOTTOM - FLUID_GAP) * PX) - bottom) * fill;
        if (top <= 1) {
            FluidBox box = new FluidBox(in, bottom, in, 1 - in, top, 1 - in);
            collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                    (pose, consumer) -> box.draw(consumer, pose, ALL, sprite, color, light));
            return;
        }
        FluidBox lower = new FluidBox(in, bottom, in, 1 - in, 1, 1 - in);
        FluidBox upper = new FluidBox(in, 0, in, 1 - in, top - 1, 1 - in);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                (pose, consumer) -> lower.draw(consumer, pose, SIDES_AND_DOWN, sprite, color, light));
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
                (pose, consumer) -> upper.draw(consumer, pose, ALL & ~DOWN, sprite, color, light));
        poseStack.popPose();
    }

    /** The locked fluid over the window of both lock plates of the upper leg block. */
    private static void plates(Level level, BlockPos upperLeg, FluidStack lock, PoseStack poseStack, SubmitNodeCollector collector) {
        TextureAtlasSprite sprite = FluidSprites.still(lock);
        int color = FluidSprites.tint(lock);
        int light = glowing(LevelRenderer.getLightCoords(level, upperLeg), lock);
        float texel = (PLATE_X1 - PLATE_X0) / 16;
        float x0 = (PLATE_X0 + WINDOW_U0 * texel) * PX;
        float x1 = (PLATE_X0 + WINDOW_U1 * texel) * PX;
        float y0 = (PLATE_Y1 - WINDOW_V1 * texel) * PX;
        float y1 = (PLATE_Y1 - WINDOW_V0 * texel) * PX;
        float south = 1 + (PLATE_OUT + PLATE_GAP) * PX;
        float north = -(PLATE_OUT + PLATE_GAP) * PX;
        FluidBox front = new FluidBox(x0, y0, north, x1, y1, south);
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, consumer) -> {
            front.quad(consumer, pose, Direction.SOUTH, sprite, color, light);
            front.quad(consumer, pose, Direction.NORTH, sprite, color, light);
        });
        poseStack.popPose();
    }

    /** The light at the leg, at least the fluid's own glow. */
    private static int glowing(int packedLight, FluidStack fluid) {
        int glow = fluid.getFluidType().getLightLevel(fluid);
        return LightCoordsUtil.pack(Math.max(LightCoordsUtil.block(packedLight), glow), LightCoordsUtil.sky(packedLight));
    }

    /** The whole frame, three blocks across the rail and two tall. */
    @Override
    public AABB getRenderBoundingBox(FluidGantryBlockEntity gantry) {
        BlockPos pos = gantry.getBlockPos();
        boolean alongX = gantry.getBlockState().getValue(FluidGantryBlock.AXIS) == Direction.Axis.X;
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + (alongX ? 1 : 3), pos.getY() + 2, pos.getZ() + (alongX ? 3 : 1))
                .inflate(PX);
    }
}
