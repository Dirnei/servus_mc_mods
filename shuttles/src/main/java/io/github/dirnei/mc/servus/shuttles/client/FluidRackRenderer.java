package io.github.dirnei.mc.servus.shuttles.client;

import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLUID_X_INSET;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLUID_Y0;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLUID_Y1;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLUID_Z0;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.FLUID_Z1;
import static io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackLook.POST;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.FluidBox;
import io.github.dirnei.mc.servus.foundation.client.LiveBlockEntityRenderer;
import io.github.dirnei.mc.servus.foundation.client.ModelPoses;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlock;
import io.github.dirnei.mc.servus.shuttles.fluidrack.FluidRackBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws what a Fluid Rack bay shows beyond its block model: the section's
 * fluid behind the glass front, from the floor up to the synced fill, the
 * same height in every bay and running through where bays join, and the
 * zone name on the top beam of the port bay.
 */
public final class FluidRackRenderer extends LiveBlockEntityRenderer<FluidRackBlockEntity> {

    private static final float PX = 1 / 16F;
    private static final double ZONE_Y = 0.93;
    private static final float ZONE_SCALE = 0.008F;

    public FluidRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void render(FluidRackBlockEntity rack, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Level level = rack.getLevel();
        BlockState state = rack.getBlockState();
        if (level == null || !(state.getBlock() instanceof FluidRackBlock)) {
            return;
        }
        Direction facing = state.getValue(FluidRackBlock.FACING);
        FluidStack fluid = rack.shownFluid();
        float fill = rack.shownFill();
        if (!fluid.isEmpty() && fill > 0) {
            poseStack.pushPose();
            ModelPoses.faceFromNorth(poseStack, facing);
            fluid(level, rack.getBlockPos(), state, fluid, fill, poseStack, collector);
            poseStack.popPose();
        }
        if (state.getValue(FluidRackBlock.PORT) && !rack.zone().isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            // Now x runs to the aisle viewer's right, y up and plus z towards the aisle.
            ZoneLabelText.draw(rack.zone(), poseStack, collector, Minecraft.getInstance().font, 0.0, ZONE_Y, 0.5 + 0.004, ZONE_SCALE,
                    LevelRenderer.getLightCoords(level, rack.getBlockPos().relative(facing)));
            poseStack.popPose();
        }
    }

    /** The fluid box in the north built bay; its side runs to the block edge where the bay joins the next one. */
    private static void fluid(Level level, BlockPos pos, BlockState state, FluidStack fluid, float fill, PoseStack poseStack,
                              SubmitNodeCollector collector) {
        boolean left = state.getValue(FluidRackBlock.LEFT);
        boolean right = state.getValue(FluidRackBlock.RIGHT);
        float x0 = left ? 0 : POST + FLUID_X_INSET;
        float x1 = right ? 16 : 16 - POST - FLUID_X_INSET;
        float top = FLUID_Y0 + (FLUID_Y1 - FLUID_Y0) * fill;
        int faces = bit(Direction.NORTH) | bit(Direction.UP) | (left ? 0 : bit(Direction.WEST)) | (right ? 0 : bit(Direction.EAST));
        FluidBox box = new FluidBox(x0 * PX, FLUID_Y0 * PX, FLUID_Z0 * PX, x1 * PX, top * PX, FLUID_Z1 * PX);
        FluidColumn.box(level, pos, pos, fluid, box, faces, poseStack, collector);
    }

    private static int bit(Direction direction) {
        return 1 << direction.ordinal();
    }
}
