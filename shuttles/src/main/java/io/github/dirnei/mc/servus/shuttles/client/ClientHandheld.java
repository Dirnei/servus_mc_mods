package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindResultPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldLockersPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldParcelPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldScanPayload;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import io.github.dirnei.mc.servus.shuttles.zone.ShelfZones;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * What the client knows for the handheld's panel and effects: shelves
 * flashing after a scan, shelves glowing after a find, the last find and
 * INFO answers, the lockers for the picker and the ready parcel. A shelf is
 * drawn as one box, the union of its bays.
 */
public final class ClientHandheld {

    /** How long a scanned shelf flashes, in ticks. */
    public static final int FLASH_TICKS = 12;
    /** How much each further shelf of a sweep raises the beep, as a pitch factor step. */
    private static final float SWEEP_PITCH_STEP = 0.06F;
    private static final float MAX_PITCH = 2.0F;

    /** A scanned shelf's box: green or red, and the game tick it was scanned at. */
    public record Flash(AABB box, boolean ok, long start) {
    }

    private static final List<Flash> FLASHES = new ArrayList<>();
    private static HandheldFindResultPayload found;
    private static List<AABB> glowing = List.of();
    private static long glowUntil;
    private static HandheldInfoPayload info;
    private static HandheldLockersPayload lockers;
    private static HandheldParcelPayload parcel;

    private ClientHandheld() {
    }

    /** The handheld in the main hand, else the one in the off hand, else empty. */
    public static ItemStack held(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof HandheldItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The bay the player aims at within the handheld's scan range, or null when it aims at no bay. */
    public static StorageNodeBlockEntity aimedBay() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : HandheldItem.aimedBay(player);
    }

    public static void scanned(HandheldScanPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || payload.bays().isEmpty()) {
            return;
        }
        FLASHES.add(new Flash(union(payload.bays()), payload.ok(), level.getGameTime()));
        float pitch = Math.min(MAX_PITCH, 1.0F + payload.sweepIndex() * SWEEP_PITCH_STEP);
        play(payload.ok() ? ServusShuttlesSounds.HANDHELD_BEEP.get() : ServusShuttlesSounds.HANDHELD_BUZZ.get(), payload.ok() ? pitch : 1.0F);
    }

    /** Keeps the answer and turns each shelf it names into its box once, from the join flags the client knows. */
    public static void found(HandheldFindResultPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        found = payload;
        List<AABB> boxes = new ArrayList<>();
        for (BlockPos shelf : payload.shelves()) {
            boxes.add(union(ShelfZones.shelf(level, shelf)));
        }
        glowing = List.copyOf(boxes);
        glowUntil = level.getGameTime() + payload.glowTicks();
        play(payload.shelves().isEmpty() ? ServusShuttlesSounds.HANDHELD_BUZZ.get() : ServusShuttlesSounds.HANDHELD_BEEP.get(), 1.0F);
    }

    public static void info(HandheldInfoPayload payload) {
        info = payload;
    }

    /** Keeps the lockers for the picker that waits for them. */
    public static void lockers(HandheldLockersPayload payload) {
        lockers = payload;
    }

    /** The lockers the server sent last, or null while the picker waits; taking them clears them. */
    public static HandheldLockersPayload takeLockers() {
        HandheldLockersPayload taken = lockers;
        lockers = null;
        return taken;
    }

    /** A parcel is ready: the panel shows it and the handheld beeps. */
    public static void parcel(HandheldParcelPayload payload) {
        parcel = payload;
        play(ServusShuttlesSounds.HANDHELD_BEEP.get(), 1.5F);
    }

    /** The ready parcel the panel shows, or null. */
    public static HandheldParcelPayload readyParcel() {
        return parcel;
    }

    /** The parcel note ends: the player sent another order or opened a locker. */
    public static void forgetParcel() {
        parcel = null;
    }

    /** Plays the mode switch click. */
    public static void clicked() {
        play(ServusShuttlesSounds.HANDHELD_CLICK.get(), 1.0F);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /** The box around all these blocks; a shelf is a straight row, so it covers just its bays. */
    private static AABB union(Collection<BlockPos> bays) {
        AABB box = null;
        for (BlockPos pos : bays) {
            box = box == null ? new AABB(pos) : box.minmax(new AABB(pos));
        }
        return box;
    }

    /** The flashing shelves; finished flashes are dropped. */
    public static List<Flash> flashes(long now) {
        FLASHES.removeIf(flash -> now - flash.start() > FLASH_TICKS);
        return FLASHES;
    }

    /** The shelves glowing after a find, empty once the glow time has passed. */
    public static List<AABB> glowing(long now) {
        if (now > glowUntil) {
            glowing = List.of();
        }
        return glowing;
    }

    /** The last find answer, or null. */
    public static HandheldFindResultPayload lastFind() {
        return found;
    }

    /** The readout of the last INFO scan, or null before the first one. */
    public static HandheldInfoPayload lastInfo() {
        return info;
    }
}
