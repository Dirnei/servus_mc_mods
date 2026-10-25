package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.dirnei.mc.servus.foundation.client.BlockSubmit;
import io.github.dirnei.mc.servus.foundation.client.RenderSubmit;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.ToppleCurve;
import io.github.dirnei.mc.servus.shuttles.field.TreeFallPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Client side: felled trees as the server sent them, drawn as one rigid group
 * that tips over the edge of its base facing away from the rail, faster and
 * faster until it lies flat; then its leaves burst and vanish in a random
 * order, its logs after them, while the drops fly in arcs to the shuttle's
 * rail. The world's blocks are already gone; this is only the look. A fall is
 * dropped when it ends or the player changes level.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class ClientFallingTrees {

    private static final ContextKey<List<Shown>> SHOWN = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "falling_trees"));
    /** The share of a fall's ticks the tipping takes; the rest is the break apart. */
    private static final double FALL_PART = 0.5;
    /** Within the break apart: leaves vanish by this share, logs start at {@link #LOGS_FROM}. */
    private static final double LEAVES_UNTIL = 0.6;
    private static final double LOGS_FROM = 0.5;
    private static final float ITEM_SCALE = 0.5F;
    private static final double ARC_HEIGHT = 1.5;
    private static final double CARGO_HEIGHT = 0.6;

    private static final List<Fall> FALLS = new ArrayList<>();

    /** One fall: the payload, the level it falls in, when it arrived and the order its blocks vanish in. */
    private static final class Fall {
        final TreeFallPayload payload;
        final ClientLevel level;
        final long start;
        final List<Integer> leaves = new ArrayList<>();
        final List<Integer> logs = new ArrayList<>();
        final int footprint;
        int leavesGone;
        int logsGone;
        boolean landed;
        /** The middle of the tree lying flat, worked out on first use. */
        Vec3 middle;

        Fall(TreeFallPayload payload, ClientLevel level, long start) {
            this.payload = payload;
            this.level = level;
            this.start = start;
            int size = 1;
            for (int i = 0; i < payload.blocks().size(); i++) {
                TreeFallPayload.FallingBlock block = payload.blocks().get(i);
                boolean log = block.state().is(BlockTags.LOGS);
                (log ? logs : leaves).add(i);
                if (log && block.dy() == 0 && (block.dx() == 1 || block.dz() == 1)) {
                    size = 2;
                }
            }
            footprint = size;
            Random random = new Random(payload.base().asLong());
            Collections.shuffle(leaves, random);
            Collections.reverse(logs);
        }

        double age(double now) {
            return (now - start) / Math.max(1, payload.ticks());
        }
    }

    /** What one fall shows this frame: how far it has tipped, how many blocks are gone, how far the drops have flown. */
    private record Shown(Fall fall, double angle, int leavesGone, int logsGone, double flight, int light) {
    }

    private ClientFallingTrees() {
    }

    /** A tree was felled near the player. */
    public static void accept(TreeFallPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            FALLS.add(new Fall(payload, level, level.getGameTime()));
        }
    }

    /** How far into the break apart the fall is, 0 to 1; negative while it still tips. */
    private static double breaking(double age) {
        return (age - FALL_PART) / (1 - FALL_PART);
    }

    private static int gone(int count, double share) {
        return (int) Math.floor(count * Math.clamp(share, 0, 1));
    }

    @SubscribeEvent
    public static void onExtract(ExtractLevelRenderStateEvent event) {
        ClientLevel level = event.getLevel();
        double now = level.getGameTime() + event.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<Shown> shown = new ArrayList<>(FALLS.size());
        for (Fall fall : FALLS) {
            double age = fall.age(now);
            if (fall.level != level || age >= 1) {
                continue;
            }
            double broken = breaking(age);
            int leavesGone = gone(fall.leaves.size(), broken / LEAVES_UNTIL);
            int logsGone = gone(fall.logs.size(), (broken - LOGS_FROM) / (1 - LOGS_FROM));
            shown.add(new Shown(fall, ToppleCurve.angle(age / FALL_PART), leavesGone, logsGone, Math.clamp(broken, 0, 1),
                    LevelRenderer.getLightCoords(level, fall.payload.base().above())));
        }
        event.getRenderState().setRenderData(SHOWN, shown.isEmpty() ? null : shown);
    }

    @SubscribeEvent
    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<Shown> shown = event.getLevelRenderState().getRenderData(SHOWN);
        if (shown == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        for (Shown fall : shown) {
            drawTree(event.getPoseStack(), event.getSubmitNodeCollector(), camera, fall);
            drawDrops(event.getPoseStack(), event.getSubmitNodeCollector(), camera, fall);
        }
    }

    /** The point on the ground the tree tips over: the middle of its base's edge facing away from the rail. */
    private static Vec3 pivot(Fall fall) {
        BlockPos base = fall.payload.base();
        Direction away = fall.payload.away();
        double half = fall.footprint / 2.0;
        double x = base.getX() + half + away.getStepX() * half;
        double z = base.getZ() + half + away.getStepZ() * half;
        return new Vec3(x, base.getY(), z);
    }

    /** The turn that tips the tree by the angle toward its away direction. */
    private static Quaternionf tip(Direction away, double angle) {
        float degrees = (float) angle;
        return switch (away) {
            case EAST -> Axis.ZP.rotationDegrees(-degrees);
            case WEST -> Axis.ZP.rotationDegrees(degrees);
            case SOUTH -> Axis.XP.rotationDegrees(degrees);
            default -> Axis.XP.rotationDegrees(-degrees);
        };
    }

    private static void drawTree(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera, Shown shown) {
        Fall fall = shown.fall();
        Vec3 pivot = pivot(fall);
        BlockPos base = fall.payload.base();
        List<TreeFallPayload.FallingBlock> blocks = fall.payload.blocks();
        boolean[] hidden = new boolean[blocks.size()];
        for (int i = 0; i < shown.leavesGone(); i++) {
            hidden[fall.leaves.get(i)] = true;
        }
        for (int i = 0; i < shown.logsGone(); i++) {
            hidden[fall.logs.get(i)] = true;
        }
        poseStack.pushPose();
        poseStack.translate(pivot.x - camera.x, pivot.y - camera.y, pivot.z - camera.z);
        poseStack.mulPose(tip(fall.payload.away(), shown.angle()));
        for (int i = 0; i < blocks.size(); i++) {
            if (hidden[i]) {
                continue;
            }
            TreeFallPayload.FallingBlock block = blocks.get(i);
            poseStack.pushPose();
            poseStack.translate(base.getX() + block.dx() - pivot.x, base.getY() + block.dy() - pivot.y, base.getZ() + block.dz() - pivot.z);
            BlockSubmit.block(poseStack, collector, block.state(), shown.light());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Where a block of the tree lies now: its centre turned with the tree. */
    private static Vec3 lying(Fall fall, TreeFallPayload.FallingBlock block, double angle) {
        Vec3 pivot = pivot(fall);
        BlockPos base = fall.payload.base();
        Vector3f offset = new Vector3f((float) (base.getX() + block.dx() + 0.5 - pivot.x), (float) (base.getY() + block.dy() + 0.5 - pivot.y),
                (float) (base.getZ() + block.dz() + 0.5 - pivot.z));
        tip(fall.payload.away(), angle).transform(offset);
        return pivot.add(offset.x(), offset.y(), offset.z());
    }

    /** The drops on arcs from the lying tree's middle to just above the rail, each a little after the one before. */
    private static void drawDrops(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera, Shown shown) {
        if (shown.flight() <= 0) {
            return;
        }
        Fall fall = shown.fall();
        List<ItemStack> drops = fall.payload.drops();
        Vec3 from = middle(fall);
        BlockPos rail = fall.payload.rail();
        Vec3 to = new Vec3(rail.getX() + 0.5, rail.getY() + CARGO_HEIGHT, rail.getZ() + 0.5);
        for (int i = 0; i < drops.size(); i++) {
            double delay = drops.size() <= 1 ? 0 : 0.5 * i / (drops.size() - 1);
            double t = Math.clamp((shown.flight() - delay) / 0.5, 0, 1);
            if (t <= 0 || t >= 1) {
                continue;
            }
            Vec3 at = from.lerp(to, t).add(0, ARC_HEIGHT * 4 * t * (1 - t), 0);
            poseStack.pushPose();
            poseStack.translate(at.x - camera.x, at.y - camera.y, at.z - camera.z);
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            RenderSubmit.item(poseStack, collector, drops.get(i), ItemDisplayContext.GROUND, fall.level, i, shown.light());
            poseStack.popPose();
        }
    }

    /** The middle of the tree lying flat. */
    private static Vec3 middle(Fall fall) {
        if (fall.middle == null) {
            List<TreeFallPayload.FallingBlock> blocks = fall.payload.blocks();
            Vec3 sum = Vec3.ZERO;
            for (TreeFallPayload.FallingBlock block : blocks) {
                sum = sum.add(lying(fall, block, ToppleCurve.FLAT));
            }
            fall.middle = blocks.isEmpty() ? pivot(fall) : sum.scale(1.0 / blocks.size());
        }
        return fall.middle;
    }

    /**
     * Each tick: the landing thud, leaf and log bursts for the blocks that
     * vanished since the last tick, and ended falls and those of another
     * level dropped.
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (FALLS.isEmpty()) {
            return;
        }
        if (level == null) {
            FALLS.clear();
            return;
        }
        double now = level.getGameTime();
        FALLS.removeIf(fall -> fall.level != level);
        for (Fall fall : FALLS) {
            double broken = breaking(fall.age(now));
            if (broken >= 0 && !fall.landed) {
                fall.landed = true;
                land(fall);
            }
            int leaves = gone(fall.leaves.size(), broken / LEAVES_UNTIL);
            for (; fall.leavesGone < leaves; fall.leavesGone++) {
                burst(fall, fall.leaves.get(fall.leavesGone));
            }
            int logs = gone(fall.logs.size(), (broken - LOGS_FROM) / (1 - LOGS_FROM));
            for (; fall.logsGone < logs; fall.logsGone++) {
                burst(fall, fall.logs.get(fall.logsGone));
            }
        }
        FALLS.removeIf(fall -> fall.age(now) >= 1);
    }

    private static void land(Fall fall) {
        List<TreeFallPayload.FallingBlock> blocks = fall.payload.blocks();
        if (fall.logs.isEmpty()) {
            return;
        }
        BlockState log = blocks.get(fall.logs.get(0)).state();
        Vec3 at = middle(fall);
        fall.level.playLocalSound(at.x, at.y, at.z, log.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.0F, 0.6F, false);
    }

    private static void burst(Fall fall, int index) {
        TreeFallPayload.FallingBlock block = fall.payload.blocks().get(index);
        Vec3 at = lying(fall, block, ToppleCurve.FLAT);
        fall.level.addDestroyBlockEffect(BlockPos.containing(at), block.state());
    }
}
