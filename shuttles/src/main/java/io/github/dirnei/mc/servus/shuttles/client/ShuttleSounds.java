package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.client.ShuttleSoundPick.Mover;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The drive sound of moving shuttles. The garage renderer reports every
 * shuttle it draws; each client tick the nearest few that move play a looped
 * drive at their position, at a pitch from their speed, damped while they are
 * muffled. A shuttle that stops, is no longer picked or no longer drawn fades out.
 */
@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID, value = Dist.CLIENT)
public final class ShuttleSounds {

    /** How many shuttles sound at once. */
    private static final int VOICES = 4;
    /** The speed in blocks per tick at which the drive plays at its own pitch. */
    private static final double REFERENCE_SPEED = 0.15;
    /** Ticks without a drawing after which a shuttle counts as gone. */
    private static final int SILENT_AFTER = 3;
    /** Ticks a drive takes to fade in or out. */
    private static final int FADE_TICKS = 8;

    /** One shuttle: its garage and its number, or its place in the garage's views when it has none yet. */
    public record Key(BlockPos garage, int shuttle) {

        long hash() {
            return garage.asLong() * 31 + shuttle;
        }
    }

    /** Where a shuttle was drawn last, how fast it went, whether it was muffled and on which tick. */
    private record Seen(double x, double y, double z, double speed, boolean muffled, long tick) {
    }

    /** A drive loop following one shuttle; it fades in when it starts and fades out before it stops. */
    private static final class Drive extends AbstractTickableSoundInstance {

        final boolean muffled;
        private float fade;
        private boolean ending;

        Drive(SoundEvent sound, boolean muffled, Seen seen) {
            super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.muffled = muffled;
            looping = true;
            delay = 0;
            volume = 0;
            follow(seen, 1);
        }

        void follow(Seen seen, float pitch) {
            x = seen.x();
            y = seen.y();
            z = seen.z();
            this.pitch = pitch;
        }

        void end() {
            ending = true;
        }

        boolean ending() {
            return ending;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            fade = Math.clamp(fade + (ending ? -1.0F : 1.0F) / FADE_TICKS, 0, 1);
            volume = fade;
            if (ending && fade == 0) {
                stop();
            }
        }
    }

    private static final Map<Key, Seen> SEEN = new HashMap<>();
    private static final Map<Key, Drive> PLAYING = new HashMap<>();
    /** Drives fading out, kept until they have stopped. */
    private static final List<Drive> ENDING = new ArrayList<>();
    private static Level seenIn;

    private ShuttleSounds() {
    }

    /**
     * A shuttle was drawn at that position, driving at that speed in blocks
     * per tick (0 while it stands), muffled or not. Called from the garage
     * renderer each time it draws a shuttle.
     */
    public static void update(Level level, Key key, double x, double y, double z, double speed, boolean muffled) {
        if (level != seenIn) {
            silenceAll();
            seenIn = level;
        }
        SEEN.put(key, new Seen(x, y, z, speed, muffled, level.getGameTime()));
    }

    /** Picks the nearest moving shuttles, starts, moves and ends their drives; everything stops when the level goes. */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (SEEN.isEmpty() && PLAYING.isEmpty() && ENDING.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level != seenIn) {
            silenceAll();
            return;
        }
        long now = level.getGameTime();
        SEEN.values().removeIf(seen -> now - seen.tick() > SILENT_AFTER || now < seen.tick());
        Set<Key> picked = new HashSet<>(pick(minecraft.gameRenderer.getMainCamera().position()));
        SoundManager sounds = minecraft.getSoundManager();
        for (Iterator<Map.Entry<Key, Drive>> it = PLAYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Key, Drive> entry = it.next();
            Seen seen = SEEN.get(entry.getKey());
            if (!picked.contains(entry.getKey()) || seen == null || seen.muffled() != entry.getValue().muffled) {
                entry.getValue().end();
                ENDING.add(entry.getValue());
                it.remove();
            }
        }
        ENDING.removeIf(Drive::isStopped);
        for (Key key : picked) {
            Seen seen = SEEN.get(key);
            Drive drive = PLAYING.get(key);
            if (drive == null) {
                drive = new Drive(seen.muffled() ? ServusShuttlesSounds.SHUTTLE_DRIVE_MUFFLED.get() : ServusShuttlesSounds.SHUTTLE_DRIVE.get(),
                        seen.muffled(), seen);
                PLAYING.put(key, drive);
                sounds.play(drive);
            }
            drive.follow(seen, ShuttleSoundPick.pitch(seen.speed(), REFERENCE_SPEED, ShuttleSoundPick.jitter(key.hash())));
        }
    }

    private static List<Key> pick(Vec3 listener) {
        List<Mover<Key>> movers = new ArrayList<>(SEEN.size());
        SEEN.forEach((key, seen) -> movers.add(new Mover<>(key, seen.x(), seen.y(), seen.z(), seen.speed())));
        return ShuttleSoundPick.nearest(movers, listener.x, listener.y, listener.z, VOICES);
    }

    private static void silenceAll() {
        SoundManager sounds = Minecraft.getInstance().getSoundManager();
        PLAYING.values().forEach(sounds::stop);
        ENDING.forEach(sounds::stop);
        PLAYING.clear();
        ENDING.clear();
        SEEN.clear();
        seenIn = null;
    }
}
