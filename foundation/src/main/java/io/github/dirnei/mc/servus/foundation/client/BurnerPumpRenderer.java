package io.github.dirnei.mc.servus.foundation.client;

import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamStroke;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpParts;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Rocks the Burner Pump's beam while it burns and moves the pump rod and the
 * piston rod with the beam's ends. Drawn from the lower half; the beam keeps
 * its pose while the pump is unlit.
 */
public class BurnerPumpRenderer implements BlockEntityRenderer<BurnerPumpBlockEntity, BurnerPumpRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> BEAM_MODEL = ModelKeys.key("block/burner_pump_beam");
    public static final StandaloneModelKey<BlockStateModelPart> ROD_MODEL = ModelKeys.key("block/burner_pump_rod");
    public static final StandaloneModelKey<BlockStateModelPart> PISTON_MODEL = ModelKeys.key("block/burner_pump_piston");

    /** Look only: twelve degrees either way, one stroke every two seconds. */
    private static final BeamStroke STROKE = new BeamStroke(12, 40);

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
        state.angle = STROKE.angle(advance(pump, blockState, partialTick));
        state.light = pump.getLevel() == null ? state.lightCoords : LevelRenderer.getLightCoords(pump.getLevel(), pump.getBlockPos().above());
        state.facing = blockState.getValue(MachineBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        float angle = state.angle;
        int light = state.light;
        float pivotX = BeamPumpParts.PIVOT_X / 16.0F;
        float pivotY = BeamPumpParts.PIVOT_Y / 16.0F;

        poseStack.pushPose();
        // The models are built facing north; turn like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((state.facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        float rod = BeamStroke.rodOffset(angle, BeamPumpParts.ROD_X - BeamPumpParts.PIVOT_X) / 16.0F;
        draw(ROD_MODEL, poseStack, collector, light, 0, rod);

        poseStack.translate(0.0F, 1.0F, 0.0F);
        float piston = BeamStroke.rodOffset(angle, BeamPumpParts.PISTON_X - BeamPumpParts.PIVOT_X) / 16.0F;
        draw(PISTON_MODEL, poseStack, collector, light, 0, piston);

        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-pivotX, -pivotY, 0.0F);
        draw(BEAM_MODEL, poseStack, collector, light, 0, 0);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void draw(StandaloneModelKey<BlockStateModelPart> key, PoseStack poseStack, SubmitNodeCollector collector,
                             int light, float dx, float dy) {
        poseStack.pushPose();
        poseStack.translate(dx, dy, 0.0F);
        ModelKeys.submit(key, poseStack, collector, light);
        poseStack.popPose();
    }

    /** Both halves, so the beam stays drawn while only the upper block is in view. */
    @Override
    public AABB getRenderBoundingBox(BurnerPumpBlockEntity pump) {
        return new AABB(pump.getBlockPos()).expandTowards(0, 1, 0);
    }

    /** Advances the stroke while lit and keeps the pose while unlit. */
    private static double advance(BurnerPumpBlockEntity pump, BlockState state, float partialTick) {
        if (pump.getLevel() == null) {
            return pump.beamPhase;
        }
        double now = pump.getLevel().getGameTime() + (double) partialTick;
        double elapsed = pump.beamTime < 0 ? 0 : Math.max(0, now - pump.beamTime);
        pump.beamTime = now;
        if (state.getValue(MachineBlock.LIT)) {
            pump.beamPhase += elapsed;
        }
        return pump.beamPhase;
    }
}
