package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.ArmPose;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Draws the felling arm of a Lumber shuttle at work, in the shuttle model's
 * space (model pixels, +z the front): the turret turns toward the trunk, the
 * arm unfolds and the claws close while it saws, the saw spins with sawdust
 * in the log's colour, and the arm folds back after the tree has come down.
 * The phases split like this: sawing is 40 percent reach, 15 percent grip and
 * 45 percent saw; the topple part holds the arm while the tree falls and
 * breaks apart and folds it back in its last 20 percent.
 */
public final class FellingArmLook {

    public static final StandaloneModelKey<BlockStateModelPart> BASE = key("lumber_base");
    public static final StandaloneModelKey<BlockStateModelPart> TURRET = key("lumber_turret");
    public static final StandaloneModelKey<BlockStateModelPart> ARM_LOWER = key("lumber_arm_lower");
    public static final StandaloneModelKey<BlockStateModelPart> ARM_UPPER = key("lumber_arm_upper");
    public static final StandaloneModelKey<BlockStateModelPart> HEAD = key("lumber_head");
    public static final StandaloneModelKey<BlockStateModelPart> CLAW = key("lumber_claw");
    public static final StandaloneModelKey<BlockStateModelPart> SAW = key("lumber_saw");

    private static final float PX = 1 / 16F;
    /** The turret turns about this point on top of the ring. */
    private static final float TURRET_X = 8;
    private static final float TURRET_Y = 12;
    private static final float TURRET_Z = 12;
    /** The shoulder sits this far up and out from the turret's pivot, turning with it. */
    private static final float SHOULDER_UP = 1;
    private static final float SHOULDER_OUT = 1;
    /** The upper arm hinges this far above the lower arm's end, so folded they lie one on the other. */
    private static final float ELBOW_UP = 1;
    private static final double REST_LENGTH = 4;
    private static final double MAX_LENGTH = 48;
    /** How far short of the trunk's centre the wrist stops, so the claws reach round the trunk. */
    private static final double TRUNK_HALF = 10;
    /** The cut's height above the plot's floor, in blocks. */
    private static final double CUT = 0.15;
    private static final float CLAW_SIDE = 1;
    private static final float CLAW_OPEN = 35;
    private static final float CLAW_STEP = 0.005F;
    private static final float SAW_DOWN = 1;
    private static final float SAW_OUT = 1.5F;
    private static final float SAW_DEGREES_PER_TICK = 40;
    private static final double REACH_SHARE = 0.4;
    private static final double GRIP_SHARE = 0.15;
    private static final double FALL_SHARE = 0.4;
    private static final double FOLD_FROM = 0.8;
    private static final int DUST_PER_TICK = 2;

    /** The game tick each trunk last got sawdust, so a trunk drawn several times a tick gets it once. */
    private static final Map<BlockPos, Long> LAST_DUST = new HashMap<>();

    private FellingArmLook() {
    }

    private static StandaloneModelKey<BlockStateModelPart> key(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttle/" + name);
        return new StandaloneModelKey<>(id::toString);
    }

    /** Registers the arm's parts for loading. */
    public static void register(ModelEvent.RegisterStandalone event) {
        for (StandaloneModelKey<BlockStateModelPart> key : List.of(BASE, TURRET, ARM_LOWER, ARM_UPPER, HEAD, CLAW, SAW)) {
            ModelKeys.register(event, key);
        }
    }

    /** Where the arm is in its motion: how far it has unfolded, how far the claws are closed, and whether the saw runs. */
    private record Motion(double reach, double grip, boolean sawing) {
    }

    private static Motion motion(ShuttleView.Felling felling, double ticksIn) {
        double t = Math.clamp(ticksIn / Math.max(1, felling.ticks()), 0, 1);
        if (felling.phase() == ShuttleView.Felling.Phase.SAW) {
            double reach = Math.clamp(t / REACH_SHARE, 0, 1);
            double grip = Math.clamp((t - REACH_SHARE) / GRIP_SHARE, 0, 1);
            return new Motion(reach, grip, t > REACH_SHARE + GRIP_SHARE);
        }
        double release = 1 - Math.clamp(t / FALL_SHARE, 0, 1);
        double fold = Math.clamp((t - FOLD_FROM) / (1 - FOLD_FROM), 0, 1);
        return new Motion(1 - fold, release, false);
    }

    /**
     * Draws the arm, with the pose stack at the shuttle model's origin.
     *
     * @param trunkModel the trunk's centre at the cut, in the shuttle model's pixels
     * @param ticksIn    ticks since the phase began
     */
    public static void draw(PoseStack poseStack, SubmitNodeCollector collector, ShuttleView.Felling felling, double[] trunkModel,
                            double ticksIn, int light) {
        Motion motion = motion(felling, ticksIn);
        double dx = trunkModel[0] - TURRET_X;
        double dz = trunkModel[2] - TURRET_Z;
        double yaw = Math.toDegrees(Math.atan2(dx, dz));
        double distance = Math.hypot(dx, dz) - SHOULDER_OUT - TRUNK_HALF;
        double drop = TURRET_Y + SHOULDER_UP - (trunkModel[1] + SAW_DOWN);
        ArmPose.Pose pose = ArmPose.of(Math.max(0, distance), drop, yaw, motion.reach(), motion.grip(), REST_LENGTH, MAX_LENGTH);

        ModelKeys.submit(BASE, poseStack, collector, light);
        poseStack.pushPose();
        poseStack.translate(TURRET_X * PX, TURRET_Y * PX, TURRET_Z * PX);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) pose.yaw()));
        part(poseStack, collector, TURRET, light);
        poseStack.translate(0, SHOULDER_UP * PX, SHOULDER_OUT * PX);

        double lower = Math.toRadians(pose.lower());
        double upper = Math.toRadians(pose.upper());
        float length = (float) pose.length();
        segment(poseStack, collector, ARM_LOWER, pose.lower(), length, light);
        float elbowY = (float) (length * Math.sin(lower) + ELBOW_UP * Math.cos(lower));
        float elbowZ = (float) (length * Math.cos(lower) - ELBOW_UP * Math.sin(lower));
        poseStack.translate(0, elbowY * PX, elbowZ * PX);
        segment(poseStack, collector, ARM_UPPER, pose.upper(), length, light);
        poseStack.translate(0, (float) (length * Math.sin(upper)) * PX, (float) (length * Math.cos(upper)) * PX);

        part(poseStack, collector, HEAD, light);
        float open = (float) (CLAW_OPEN * pose.clawOpening());
        claw(poseStack, collector, -CLAW_SIDE, -open, 0, light);
        claw(poseStack, collector, CLAW_SIDE, open, CLAW_STEP, light);
        poseStack.pushPose();
        poseStack.translate(0, -SAW_DOWN * PX, SAW_OUT * PX);
        if (motion.sawing()) {
            poseStack.mulPose(Axis.YP.rotationDegrees((float) (ticksIn * SAW_DEGREES_PER_TICK % 360)));
        }
        part(poseStack, collector, SAW, light);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** A part built around the model's centre, drawn at the pose's origin. */
    private static void part(PoseStack poseStack, SubmitNodeCollector collector, StandaloneModelKey<BlockStateModelPart> key, int light) {
        poseStack.pushPose();
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        ModelKeys.submit(key, poseStack, collector, light);
        poseStack.popPose();
    }

    /** An arm part raised by the angle and stretched along +z to the length, both in model pixels. */
    private static void segment(PoseStack poseStack, SubmitNodeCollector collector, StandaloneModelKey<BlockStateModelPart> key,
                                double angle, float length, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -angle));
        poseStack.scale(1, 1, length / 16);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        ModelKeys.submit(key, poseStack, collector, light);
        poseStack.popPose();
    }

    private static void claw(PoseStack poseStack, SubmitNodeCollector collector, float side, float open, float step, int light) {
        poseStack.pushPose();
        poseStack.translate(side * PX, step * PX, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(open));
        part(poseStack, collector, CLAW, light);
        poseStack.popPose();
    }

    /** A few grains of sawdust in the log's colour at the cut, once per tick while the saw runs. */
    public static void sawdust(Level level, ShuttleView.Felling felling, double ticksIn) {
        BlockPos trunk = felling.trunk();
        if (!motion(felling, ticksIn).sawing()) {
            LAST_DUST.remove(trunk);
            return;
        }
        Long last = LAST_DUST.put(trunk, level.getGameTime());
        if (last != null && last == level.getGameTime()) {
            return;
        }
        BlockState log = level.getBlockState(trunk);
        if (!log.is(BlockTags.LOGS)) {
            return;
        }
        RandomSource random = level.getRandom();
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.FALLING_DUST, log);
        for (int i = 0; i < DUST_PER_TICK; i++) {
            level.addParticle(dust, trunk.getX() + random.nextDouble(), trunk.getY() + CUT + random.nextDouble() * 0.1,
                    trunk.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }

    /** The cut's height above the rail's floor, in blocks. */
    public static double cutHeight() {
        return CUT;
    }
}
