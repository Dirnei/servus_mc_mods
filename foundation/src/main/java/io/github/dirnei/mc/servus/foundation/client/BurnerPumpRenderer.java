package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamStroke;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.StrokeClock;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

/**
 * Rocks the Burner Pump's beam while it burns and moves the pump rod and the
 * piston rod with the beam's ends, from the stroke phase its block entity
 * keeps on the client. Drawn from the lower half; the beam keeps its pose
 * while the pump is unlit.
 */
public class BurnerPumpRenderer implements BlockEntityRenderer<BurnerPumpBlockEntity, BurnerPumpRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> BEAM_MODEL = ModelKeys.key("block/burner_pump_beam");
    public static final StandaloneModelKey<BlockStateModelPart> ROD_MODEL = ModelKeys.key("block/burner_pump_rod");
    public static final StandaloneModelKey<BlockStateModelPart> PISTON_MODEL = ModelKeys.key("block/burner_pump_piston");

    /** The beam angle and lighting of one frame. */
    public static final class State extends BlockEntityRenderState {
        float angle;
        int light;
        Direction facing = Direction.NORTH;
    }

    public BurnerPumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BurnerPumpBlockEntity pump, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pump, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = pump.getBlockState();
        state.angle = StrokeClock.STROKE.angle(pump.strokeClock().phase(partialTick, blockState.getValue(MachineBlock.LIT)));
        state.light = PumpRenders.upperLight(pump, state.lightCoords);
        state.facing = blockState.getValue(MachineBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float angle = state.angle;
        int light = state.light;
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, state.facing);

        float rod = BeamStroke.rodOffset(angle, BeamPumpParts.ROD_X - BeamPumpParts.PIVOT_X) / 16.0F;
        ModelPoses.translated(poseStack, 0, rod, 0, () -> ModelKeys.submit(ROD_MODEL, poseStack, collector, light));

        poseStack.translate(0.0F, 1.0F, 0.0F);
        float piston = BeamStroke.rodOffset(angle, BeamPumpParts.PISTON_X - BeamPumpParts.PIVOT_X) / 16.0F;
        ModelPoses.translated(poseStack, 0, piston, 0, () -> ModelKeys.submit(PISTON_MODEL, poseStack, collector, light));

        ModelPoses.rotateAbout(poseStack, BeamPumpParts.PIVOT_X, BeamPumpParts.PIVOT_Y, 0, Axis.ZP, angle,
                () -> ModelKeys.submit(BEAM_MODEL, poseStack, collector, light));
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BurnerPumpBlockEntity pump) {
        return PumpRenders.bothHalves(pump);
    }
}
