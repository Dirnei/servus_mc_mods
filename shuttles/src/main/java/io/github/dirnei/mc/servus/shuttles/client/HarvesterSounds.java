package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterSound;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterSound.Cue;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The sounds of each Lumber shuttle's harvester, kept per rail it stands on
 * while it is unfolded: every cue of {@link HarvesterSound} plays once, the
 * chainsaw's once per tree, and the idle loops at the saw whenever the
 * timeline calls for it and no chainsaw cue is still sounding. A shuttle no
 * longer drawn falls silent.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class HarvesterSounds {

    /** Ticks without a drawing after which a shuttle falls silent. */
    private static final int SILENT_AFTER = 3;
    /** Ticks without a drawing after which a shuttle's played cues are forgotten. */
    private static final int FORGET_AFTER = 1200;
    /** The lengths of the chainsaw's cut and wind-down recordings, in ticks. */
    private static final int CUT_TICKS = 64;
    private static final int STOP_TICKS = 32;

    /** The idle, looped and moved along with the saw. */
    private static final class Idle extends AbstractTickableSoundInstance {

        Idle(double[] at) {
            super(ServusShuttlesSounds.CHAINSAW_IDLE.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            moveTo(at);
        }

        void moveTo(double[] at) {
            x = at[0];
            y = at[1];
            z = at[2];
        }

        void end() {
            stop();
        }

        @Override
        public void tick() {
        }
    }

    private static final class Stand {
        final Set<String> played = new HashSet<>();
        Idle idle;
        SoundInstance chainsaw;
        long chainsawUntil;
        long drawn;
    }

    private static final Map<BlockPos, Stand> STANDS = new HashMap<>();

    private HarvesterSounds() {
    }

    /**
     * The sounds for a felling shuttle standing at the rail, sawing the trunk,
     * at that part and moment; the saw and the hold are where in the world the
     * chainsaw and the hold's sounds play. Called each time the shuttle is drawn.
     */
    public static void update(Level level, BlockPos rail, BlockPos trunk, HarvesterTimeline.Part part, HarvesterTimeline.Moment moment,
                              double[] saw, double[] hold) {
        Stand stand = STANDS.computeIfAbsent(rail.immutable(), pos -> new Stand());
        long now = level.getGameTime();
        stand.drawn = now;
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        HarvesterSound.cue(part, moment).ifPresent(cue -> play(stand, cue, trunk, now, saw, hold, sounds));
        boolean idles = HarvesterSound.idles(part, moment) && now >= stand.chainsawUntil;
        if (!idles) {
            quietIdle(stand, sounds);
        } else if (stand.idle == null) {
            stand.idle = new Idle(saw);
            sounds.play(stand.idle);
        } else {
            stand.idle.moveTo(saw);
        }
    }

    private static void play(Stand stand, Cue cue, BlockPos trunk, long now, double[] saw, double[] hold, SoundManager sounds) {
        if (cue == Cue.DOORS_OPEN && stand.played.contains(Cue.DOORS_SHUT.name())) {
            stand.played.clear();
        }
        if (!stand.played.add(cue.chainsaw() ? cue.name() + trunk.asLong() : cue.name())) {
            return;
        }
        if (!cue.chainsaw()) {
            sounds.play(at(sound(cue), hold));
            return;
        }
        quietIdle(stand, sounds);
        if (stand.chainsaw != null) {
            sounds.stop(stand.chainsaw);
        }
        stand.chainsaw = at(sound(cue), saw);
        stand.chainsawUntil = now + (cue == Cue.CUT ? CUT_TICKS : STOP_TICKS);
        sounds.play(stand.chainsaw);
    }

    private static SoundInstance at(SoundEvent sound, double[] at) {
        return new SimpleSoundInstance(sound, SoundSource.BLOCKS, 1, 1, SoundInstance.createUnseededRandom(), at[0], at[1], at[2]);
    }

    private static SoundEvent sound(Cue cue) {
        return switch (cue) {
            case DOORS_OPEN -> ServusShuttlesSounds.HOLD_DOORS_OPEN.get();
            case WALLS_DOWN -> ServusShuttlesSounds.HOLD_WALLS_DOWN.get();
            case CRANE_UP -> ServusShuttlesSounds.CRANE_UP.get();
            case CUT -> ServusShuttlesSounds.CHAINSAW_CUT.get();
            case STOP -> ServusShuttlesSounds.CHAINSAW_STOP.get();
            case CRANE_DOWN -> ServusShuttlesSounds.CRANE_DOWN.get();
            case WALLS_UP -> ServusShuttlesSounds.HOLD_WALLS_UP.get();
            case DOORS_SHUT -> ServusShuttlesSounds.HOLD_DOORS_SHUT.get();
        };
    }

    private static void quietIdle(Stand stand, SoundManager sounds) {
        if (stand.idle != null) {
            stand.idle.end();
            sounds.stop(stand.idle);
            stand.idle = null;
        }
    }

    private static void quiet(Stand stand) {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        quietIdle(stand, sounds);
        if (stand.chainsaw != null) {
            sounds.stop(stand.chainsaw);
            stand.chainsaw = null;
        }
        stand.chainsawUntil = 0;
    }

    /** Silences shuttles that were not drawn lately and forgets the ones long gone; everything stops when the level goes. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (STANDS.isEmpty()) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            STANDS.values().forEach(HarvesterSounds::quiet);
            STANDS.clear();
            return;
        }
        long now = level.getGameTime();
        STANDS.values().removeIf(stand -> {
            long since = now - stand.drawn;
            if (since > SILENT_AFTER || since < 0) {
                quiet(stand);
            }
            return since > FORGET_AFTER || since < 0;
        });
    }
}
