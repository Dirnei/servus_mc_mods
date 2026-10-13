package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlock;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamPumpParts;
import io.github.dirnei.mc.servus.foundation.machine.pump.BeamStroke;
import io.github.dirnei.mc.servus.foundation.machine.pump.CrankDrive;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.ElectricPumpParts;
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
 * Turns the Electric Pump's crank wheel while it works: the drive belt loops
 * from under the motor pulley up over the axle pulley with its lacing marks
 * running round the whole loop, the wheel turns with its hub, spokes, rim and
 * axle, the con rod follows the crank pin up to the beam, the beam rocks about
 * its pivot, the pump rod follows the beam's other end and the fan spins
 * behind its grille, all from the stroke phase its block entity keeps on the
 * client. Drawn from the lower half; every part keeps its pose while the pump
 * stands.
 */
public class ElectricPumpRenderer implements BlockEntityRenderer<ElectricPumpBlockEntity, ElectricPumpRenderer.State> {

    public static final StandaloneModelKey<BlockStateModelPart> BEAM_MODEL = ModelKeys.key("block/electric_pump_beam");
    public static final StandaloneModelKey<BlockStateModelPart> ROD_MODEL = ModelKeys.key("block/electric_pump_rod");
    public static final StandaloneModelKey<BlockStateModelPart> CON_ROD_MODEL = ModelKeys.key("block/electric_pump_con_rod");
    public static final StandaloneModelKey<BlockStateModelPart> WHEEL_MODEL = ModelKeys.key("block/electric_pump_wheel");
    public static final StandaloneModelKey<BlockStateModelPart> SPOKE_MODEL = ModelKeys.key("block/electric_pump_spoke");
    public static final StandaloneModelKey<BlockStateModelPart> FAN_MODEL = ModelKeys.key("block/electric_pump_fan");
    public static final StandaloneModelKey<BlockStateModelPart> RIM_MODEL = ModelKeys.key("block/electric_pump_rim");
    public static final StandaloneModelKey<BlockStateModelPart> AXLE_MODEL = ModelKeys.key("block/electric_pump_axle");
    public static final StandaloneModelKey<BlockStateModelPart> MOTOR_PULLEY_MODEL = ModelKeys.key("block/electric_pump_motor_pulley");
    public static final StandaloneModelKey<BlockStateModelPart> BELT_MODEL = ModelKeys.key("block/electric_pump_belt");
    public static final StandaloneModelKey<BlockStateModelPart> BELT_MARK_MODEL = ModelKeys.key("block/electric_pump_belt_mark");

    private static final CrankDrive CRANK = new CrankDrive(StrokeClock.STROKE, StrokeClock.PERIOD, ElectricPumpParts.PIN_R);

    /** How far each belt arc piece reaches past its ends, in pixels, so the joints stay closed. */
    private static final float OVERLAP = 0.06F;

    /**
     * The belt's static pieces as {x1, y1, x2, y2} in pixels, lower half frame:
     * both straight strands from tangent point to tangent point, and the arcs
     * over the axle pulley's top and under the motor pulley's bottom as short
     * chords.
     */
    private static final float[][] BELT_PIECES = beltPieces();

    /** The angles of one frame and its lighting. */
    public static final class State extends BlockEntityRenderState {
        float beam;
        float crank;
        float fan;
        float pinX;
        float pinY;
        float motor;
        final float[] marks = new float[2];
        int light;
        Direction facing = Direction.NORTH;
    }

    public ElectricPumpRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ElectricPumpBlockEntity pump, State state, float partialTick, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(pump, state, partialTick, cameraPosition, breakProgress);
        BlockState blockState = pump.getBlockState();
        double phase = pump.strokeClock().phase(partialTick, blockState.getValue(MachineBlock.LIT));
        state.beam = CRANK.beamAngle(phase);
        state.crank = CRANK.crankAngle(phase);
        state.fan = CRANK.fanAngle(phase);
        state.pinX = CRANK.pinX(phase);
        state.pinY = CRANK.pinY(phase);
        double turns = phase / StrokeClock.PERIOD;
        state.motor = ElectricPumpParts.DRIVE.driverAngle(turns);
        for (int i = 0; i < state.marks.length; i++) {
            state.marks[i] = ElectricPumpParts.DRIVE.markOnLoop(turns, ElectricPumpParts.DRIVE.loopLength() * i / state.marks.length);
        }
        state.light = PumpRenders.upperLight(pump, state.lightCoords);
        state.facing = blockState.getValue(MachineBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.light;
        poseStack.pushPose();
        ModelPoses.faceFromNorth(poseStack, state.facing);

        // Rod, motor pulley and belt are placed in the lower half's frame.
        float rod = BeamStroke.rodOffset(state.beam, BeamPumpParts.ROD_X - BeamPumpParts.PIVOT_X) / 16.0F;
        ModelPoses.translated(poseStack, 0, rod, 0, () -> ModelKeys.submit(ROD_MODEL, poseStack, collector, light));
        ModelPoses.rotateAbout(poseStack, ElectricPumpParts.MOTOR_X, ElectricPumpParts.MOTOR_Y, 0, Axis.ZP, state.motor,
                () -> ModelKeys.submit(MOTOR_PULLEY_MODEL, poseStack, collector, light));
        for (int i = 0; i < BELT_PIECES.length; i++) {
            float[] piece = BELT_PIECES[i];
            ModelPoses.translated(poseStack, 0, 0, depth(i), () -> strap(piece, BELT_MODEL, poseStack, collector, light));
        }
        for (float mark : state.marks) {
            mark(mark, poseStack, collector, light);
        }

        // Everything else is placed in the upper half's frame.
        poseStack.translate(0.0F, 1.0F, 0.0F);
        ModelPoses.rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP, state.crank,
                () -> wheel(poseStack, collector, light));
        conRod(state, poseStack, collector, light);
        ModelPoses.rotateAbout(poseStack, BeamPumpParts.PIVOT_X, BeamPumpParts.PIVOT_Y, BeamPumpParts.PIVOT_Z, Axis.ZP, state.beam,
                () -> ModelKeys.submit(BEAM_MODEL, poseStack, collector, light));
        ModelPoses.rotateAbout(poseStack, ElectricPumpParts.FAN_X, ElectricPumpParts.FAN_Y, ElectricPumpParts.FAN_Z, Axis.XP, state.fan,
                () -> ModelKeys.submit(FAN_MODEL, poseStack, collector, light));

        poseStack.popPose();
    }

    /** The {@code index}th of several stacked parts, each a step further back, in blocks. */
    private static float depth(int index) {
        return -index * ElectricPumpParts.DEPTH_STEP / 16.0F;
    }

    /** The crank wheel with axle, spokes and rim segments, in the wheel's own turn. */
    private static void wheel(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        ModelKeys.submit(WHEEL_MODEL, poseStack, collector, light);
        ModelKeys.submit(AXLE_MODEL, poseStack, collector, light);
        for (int i = 0; i < ElectricPumpParts.SPOKES; i++) {
            ModelPoses.rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP,
                    360.0F * i / ElectricPumpParts.SPOKES, () -> ModelKeys.submit(SPOKE_MODEL, poseStack, collector, light));
        }
        for (int i = 0; i < ElectricPumpParts.RIM_SEGMENTS; i++) {
            float depth = depth(i);
            ModelPoses.rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP,
                    360.0F * i / ElectricPumpParts.RIM_SEGMENTS,
                    () -> ModelPoses.translated(poseStack, 0, 0, depth, () -> ModelKeys.submit(RIM_MODEL, poseStack, collector, light)));
        }
    }

    /**
     * The con rod model stands straight from the pin at its top to the level
     * beam; here it is tilted and stretched to reach from the pin where it is
     * to the beam end where it is.
     */
    private static void conRod(State state, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        float[] rod = ElectricPumpParts.CON_ROD;
        float baseX = ElectricPumpParts.CRANK_X;
        float baseY = rod[1];
        float length = rod[4] - rod[1];
        float pinX = ElectricPumpParts.CRANK_X + state.pinX;
        float pinY = ElectricPumpParts.CRANK_Y + state.pinY;
        float arm = BeamPumpParts.PISTON_X - BeamPumpParts.PIVOT_X;
        double beam = Math.toRadians(state.beam);
        float endX = (float) (BeamPumpParts.PIVOT_X + arm * Math.cos(beam));
        float endY = (float) (BeamPumpParts.PIVOT_Y + arm * Math.sin(beam));
        float dx = endX - pinX;
        float dy = endY - pinY;
        float reach = (float) Math.sqrt(dx * dx + dy * dy);
        poseStack.pushPose();
        poseStack.translate(pinX / 16.0F, pinY / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation((float) Math.atan2(-dx, dy)));
        poseStack.scale(1.0F, reach / length, 1.0F);
        poseStack.translate(-baseX / 16.0F, -baseY / 16.0F, 0.0F);
        ModelKeys.submit(CON_ROD_MODEL, poseStack, collector, light);
        poseStack.popPose();
    }

    private static float[][] beltPieces() {
        int segments = ElectricPumpParts.BELT_ARC_SEGMENTS;
        float[][] pieces = new float[2 + 2 * segments][];
        pieces[0] = ElectricPumpParts.DRIVE.strand(0);
        pieces[1] = ElectricPumpParts.DRIVE.strand(1);
        for (int pulley = 0; pulley < 2; pulley++) {
            for (int i = 0; i < segments; i++) {
                float[] a = ElectricPumpParts.DRIVE.arcPoint(pulley, (double) i / segments);
                float[] b = ElectricPumpParts.DRIVE.arcPoint(pulley, (double) (i + 1) / segments);
                float dx = b[0] - a[0];
                float dy = b[1] - a[1];
                float stretch = OVERLAP / (float) Math.hypot(dx, dy);
                pieces[2 + pulley * segments + i] = new float[] {
                        a[0] - dx * stretch, a[1] - dy * stretch, b[0] + dx * stretch, b[1] + dy * stretch};
            }
        }
        return pieces;
    }

    /**
     * One belt piece: the strand model stands straight up from (8, 0) for a
     * block; here it is tilted and stretched from one end of the piece to the other.
     */
    private static void strap(float[] piece, StandaloneModelKey<BlockStateModelPart> model, PoseStack poseStack,
                              SubmitNodeCollector collector, int light) {
        float dx = piece[2] - piece[0];
        float dy = piece[3] - piece[1];
        poseStack.pushPose();
        poseStack.translate(piece[0] / 16.0F, piece[1] / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation((float) Math.atan2(-dx, dy)));
        poseStack.scale(1.0F, (float) Math.hypot(dx, dy) / 16.0F, 1.0F);
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        ModelKeys.submit(model, poseStack, collector, light);
        poseStack.popPose();
    }

    /** A lacing mark where the belt has carried it on the loop, on a strand or round a pulley, turned along the belt. */
    private static void mark(float distance, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        float[] point = ElectricPumpParts.DRIVE.pointOnLoop(distance);
        poseStack.pushPose();
        poseStack.translate(point[0] / 16.0F, point[1] / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(point[2] - (float) Math.PI / 2));
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        ModelKeys.submit(BELT_MARK_MODEL, poseStack, collector, light);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ElectricPumpBlockEntity pump) {
        return PumpRenders.bothHalves(pump);
    }
}
