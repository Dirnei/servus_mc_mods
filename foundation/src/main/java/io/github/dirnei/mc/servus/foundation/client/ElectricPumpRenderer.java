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
import net.minecraft.client.renderer.LevelRenderer;
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
 * axle, the con rod follows the crank pin up
 * to the beam, the beam rocks about its pivot, the pump rod follows the beam's
 * other end and the fan spins behind its grille, all from the stroke phase its
 * block entity keeps on the client. Drawn from the lower
 * half; every part keeps its pose while the pump stands.
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
        state.light = pump.getLevel() == null ? state.lightCoords : LevelRenderer.getLightCoords(pump.getLevel(), pump.getBlockPos().above());
        state.facing = blockState.getValue(MachineBlock.FACING);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.light;
        poseStack.pushPose();
        // The models are built facing north; turn like the blockstate does.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-((state.facing.toYRot() + 180.0F) % 360.0F)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // The pump rod hangs from the beam's short end, in the lower half's frame.
        poseStack.pushPose();
        poseStack.translate(0.0F, BeamStroke.rodOffset(state.beam, BeamPumpParts.ROD_X - BeamPumpParts.PIVOT_X) / 16.0F, 0.0F);
        ModelKeys.submit(ROD_MODEL, poseStack, collector, light);
        poseStack.popPose();

        // The motor pulley and the belt up to the crank axle, also in the lower half's frame.
        rotateAbout(poseStack, ElectricPumpParts.MOTOR_X, ElectricPumpParts.MOTOR_Y, 0, Axis.ZP, state.motor,
                () -> ModelKeys.submit(MOTOR_PULLEY_MODEL, poseStack, collector, light));
        for (int i = 0; i < BELT_PIECES.length; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, -i * ElectricPumpParts.DEPTH_STEP / 16.0F);
            strap(BELT_PIECES[i], BELT_MODEL, poseStack, collector, light);
            poseStack.popPose();
        }
        for (float mark : state.marks) {
            mark(mark, poseStack, collector, light);
        }

        poseStack.translate(0.0F, 1.0F, 0.0F);
        // Everything else lives in the upper half's frame.
        rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP, state.crank, () -> {
            ModelKeys.submit(WHEEL_MODEL, poseStack, collector, light);
            ModelKeys.submit(AXLE_MODEL, poseStack, collector, light);
            for (int i = 0; i < ElectricPumpParts.SPOKES; i++) {
                rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP,
                        360.0F * i / ElectricPumpParts.SPOKES, () -> ModelKeys.submit(SPOKE_MODEL, poseStack, collector, light));
            }
            for (int i = 0; i < ElectricPumpParts.RIM_SEGMENTS; i++) {
                float depth = -i * ElectricPumpParts.DEPTH_STEP / 16.0F;
                rotateAbout(poseStack, ElectricPumpParts.CRANK_X, ElectricPumpParts.CRANK_Y, 0, Axis.ZP,
                        360.0F * i / ElectricPumpParts.RIM_SEGMENTS, () -> {
                            poseStack.pushPose();
                            poseStack.translate(0.0F, 0.0F, depth);
                            ModelKeys.submit(RIM_MODEL, poseStack, collector, light);
                            poseStack.popPose();
                        });
            }
        });
        conRod(state, poseStack, collector, light);
        rotateAbout(poseStack, BeamPumpParts.PIVOT_X, BeamPumpParts.PIVOT_Y, BeamPumpParts.PIVOT_Z, Axis.ZP, state.beam,
                () -> ModelKeys.submit(BEAM_MODEL, poseStack, collector, light));
        rotateAbout(poseStack, ElectricPumpParts.FAN_X, ElectricPumpParts.FAN_Y, ElectricPumpParts.FAN_Z, Axis.XP, state.fan,
                () -> ModelKeys.submit(FAN_MODEL, poseStack, collector, light));

        poseStack.popPose();
    }

    /** Draws a model turned by {@code degrees} about an axis through a point given in pixels. */
    private static void rotateAbout(PoseStack poseStack, float x, float y, float z, Axis axis, float degrees, Runnable draw) {
        poseStack.pushPose();
        poseStack.translate(x / 16.0F, y / 16.0F, z / 16.0F);
        poseStack.mulPose(axis.rotationDegrees(degrees));
        poseStack.translate(-x / 16.0F, -y / 16.0F, -z / 16.0F);
        draw.run();
        poseStack.popPose();
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

    /** How far each arc piece reaches past its ends, in pixels. */
    private static final float OVERLAP = 0.06F;

    /**
     * The belt's static pieces as {x1, y1, x2, y2}, lower half frame: both
     * straight strands from tangent point to tangent point, and the arcs over
     * the axle pulley's top and under the motor pulley's bottom as short
     * chords, each a little longer than its share so the joints stay closed.
     */
    private static final float[][] BELT_PIECES = beltPieces();

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

    /** Both halves, so the beam stays drawn while only the upper block is in view. */
    @Override
    public AABB getRenderBoundingBox(ElectricPumpBlockEntity pump) {
        return new AABB(pump.getBlockPos()).expandTowards(0, 1, 0);
    }
}
