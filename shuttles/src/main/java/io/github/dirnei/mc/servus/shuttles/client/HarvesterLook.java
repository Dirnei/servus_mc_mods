package io.github.dirnei.mc.servus.shuttles.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.dirnei.mc.servus.foundation.client.ModelKeys;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.Frame;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterParts;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterPose;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline;
import io.github.dirnei.mc.servus.shuttles.shuttle.ShuttleView;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ModuleSpec;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleLook;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleModules;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.joml.Matrix4f;

/**
 * Draws an unfolded Lumber shuttle in the shuttle model's space (model
 * pixels, +z the front): its drive and the open hold's floor, then the
 * folding walls and doors, the crane and the harvester head, each part posed
 * by {@link HarvesterPose} at the moment {@link HarvesterTimeline} gives for
 * the felling phase. Sawdust in the log's colour flies while the bar cuts.
 */
public final class HarvesterLook {

    private static final float PX = 1 / 16F;
    /** The cut's height above the plot's floor, in blocks. */
    private static final double CUT = 0.1;
    private static final int DUST_PER_TICK = 2;

    /** The posed parts' models by name, the hold's per tier. */
    private static final Map<String, StandaloneModelKey<BlockStateModelPart>> PARTS = new HashMap<>();
    /** The drive parts, drawn in place under the unfolded hold. */
    private static final Map<String, StandaloneModelKey<BlockStateModelPart>> DRIVES = new HashMap<>();

    static {
        for (HarvesterParts.Part part : HarvesterParts.Part.values()) {
            for (int tier = 1; tier <= (part.tiered() ? 3 : 1); tier++) {
                PARTS.put(part.model(tier), key(part.model(tier)));
            }
        }
        for (int step = 0; step < ShuttleLook.DRIVE_STEPS; step++) {
            DRIVES.put("drive_" + step, key("drive_" + step));
        }
        String lumberDrive = ShuttleLook.drivePart(ShuttleLook.Key.of(1, Integer.MAX_VALUE, 0, ModuleSpec.LUMBER));
        DRIVES.put(lumberDrive, key(lumberDrive));
    }

    /** The game tick each trunk last got sawdust, so a trunk drawn several times a tick gets it once. */
    private static final Map<BlockPos, Long> LAST_DUST = new HashMap<>();

    private HarvesterLook() {
    }

    private static StandaloneModelKey<BlockStateModelPart> key(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "shuttle/" + name);
        return new StandaloneModelKey<>(id::toString);
    }

    /** Registers the posed parts and the drives for loading. */
    public static void register(ModelEvent.RegisterStandalone event) {
        PARTS.values().forEach(key -> ModelKeys.register(event, key));
        DRIVES.values().forEach(key -> ModelKeys.register(event, key));
    }

    /** The felling part the view is in. */
    public static HarvesterTimeline.Part part(ShuttleView.Felling felling) {
        return HarvesterTimeline.Part.valueOf(felling.phase().name());
    }

    /** Where in its part the felling is, {@code ticksIn} ticks after the part began. */
    public static HarvesterTimeline.Moment moment(ShuttleView.Felling felling, double ticksIn) {
        return HarvesterTimeline.of(part(felling), ticksIn / Math.max(1, felling.ticks()));
    }

    /** Where the chain bar's pivot is, in the shuttle model's pixels. */
    public static double[] saw(ShuttleView.Felling felling, double[] log, double ticksIn) {
        return HarvesterPose.of(log, moment(felling, ticksIn)).head().apply(HarvesterParts.BAR_PIVOT);
    }

    /**
     * Draws the unfolded shuttle, with the pose stack at the shuttle model's origin.
     *
     * @param log     the gripped log's centre at the plot's floor, in the shuttle model's pixels
     * @param ticksIn ticks since the phase began
     */
    public static void draw(PoseStack poseStack, SubmitNodeCollector collector, ItemStack cart, ShuttleView.Felling felling, double[] log,
                            double ticksIn, int light) {
        ShuttleModules modules = ShuttleModules.of(cart);
        ShuttleLook.Key look = ShuttleLook.Key.of(ShuttleItem.tier(cart), modules.speed(), modules.handling(), ModuleSpec.LUMBER);
        int tier = ShuttleLook.tier(look);
        ModelKeys.submit(DRIVES.get(ShuttleLook.drivePart(look)), poseStack, collector, light);
        HarvesterPose pose = HarvesterPose.of(log, moment(felling, ticksIn));
        poseStack.pushPose();
        poseStack.scale(PX, PX, PX);
        for (HarvesterPose.Placed placed : pose.parts()) {
            poseStack.pushPose();
            poseStack.mulPose(matrix(placed.frame()));
            if (placed.part().centred()) {
                float centre = (float) HarvesterParts.CENTRE;
                poseStack.translate(-centre, -centre, -centre);
            }
            poseStack.scale(16, 16, 16);
            ModelKeys.submit(PARTS.get(placed.part().model(tier)), poseStack, collector, light);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static Matrix4f matrix(Frame frame) {
        double[][] m = frame.m();
        double[] t = frame.t();
        return new Matrix4f(
                (float) m[0][0], (float) m[1][0], (float) m[2][0], 0,
                (float) m[0][1], (float) m[1][1], (float) m[2][1], 0,
                (float) m[0][2], (float) m[1][2], (float) m[2][2], 0,
                (float) t[0], (float) t[1], (float) t[2], 1);
    }

    /** A few grains of sawdust in the log's colour at the cut, once per tick while the bar cuts. */
    public static void sawdust(Level level, ShuttleView.Felling felling, double ticksIn) {
        BlockPos trunk = felling.trunk();
        if (felling.phase() != ShuttleView.Felling.Phase.SAW || moment(felling, ticksIn).stage() != HarvesterTimeline.Stage.CUT) {
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
}
