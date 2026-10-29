package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindResultPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoRequest;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldItem;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldScanPayload;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesSounds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * What the client knows for the handheld's panel and effects: bays flashing
 * after a scan, bays glowing after a FIND, the last FIND and INFO answers,
 * and the throttled INFO requests.
 */
public final class ClientHandheld {

    /** How long a scanned bay flashes, in ticks. */
    public static final int FLASH_TICKS = 12;
    /** The fewest ticks between two INFO requests. */
    private static final int INFO_REQUEST_TICKS = 10;
    /** How much each further bay of a sweep raises the beep, as a pitch factor step. */
    private static final float SWEEP_PITCH_STEP = 0.06F;
    private static final float MAX_PITCH = 2.0F;

    /** A scanned bay: green or red, and the game tick it was scanned at. */
    public record Flash(boolean ok, long start) {
    }

    private static final Map<BlockPos, Flash> FLASHES = new HashMap<>();
    private static HandheldFindResultPayload found;
    private static List<BlockPos> glowing = List.of();
    private static long glowUntil;
    private static HandheldInfoPayload info;
    private static BlockPos requested;
    private static long requestedAt;

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

    /** The bay under the crosshair, or null when the player aims at no bay. */
    public static StorageNodeBlockEntity aimedBay() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof StorageNodeBlockEntity bay && bay.zonable() ? bay : null;
    }

    public static void scanned(HandheldScanPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        for (BlockPos pos : payload.bays()) {
            FLASHES.put(pos, new Flash(payload.ok(), now));
        }
        float pitch = Math.min(MAX_PITCH, 1.0F + payload.sweepIndex() * SWEEP_PITCH_STEP);
        play(payload.ok() ? ServusShuttlesSounds.HANDHELD_BEEP.get() : ServusShuttlesSounds.HANDHELD_BUZZ.get(), payload.ok() ? pitch : 1.0F);
    }

    public static void found(HandheldFindResultPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        found = payload;
        glowing = List.copyOf(payload.bays());
        glowUntil = level.getGameTime() + payload.glowTicks();
        play(payload.bays().isEmpty() ? ServusShuttlesSounds.HANDHELD_BUZZ.get() : ServusShuttlesSounds.HANDHELD_BEEP.get(), 1.0F);
    }

    public static void info(HandheldInfoPayload payload) {
        info = payload;
    }

    /** Plays the mode switch click. */
    public static void clicked() {
        play(ServusShuttlesSounds.HANDHELD_CLICK.get(), 1.0F);
    }

    private static void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    /** The flashing bays with how far each flash has faded, 0 fresh to 1 gone; finished flashes are dropped. */
    public static Map<BlockPos, Flash> flashes(long now) {
        FLASHES.values().removeIf(flash -> now - flash.start() > FLASH_TICKS);
        return FLASHES;
    }

    /** The bays glowing after a FIND, empty once the glow time has passed. */
    public static List<BlockPos> glowing(long now) {
        if (now > glowUntil) {
            glowing = List.of();
        }
        return glowing;
    }

    /** The last FIND answer, or null. */
    public static HandheldFindResultPayload lastFind() {
        return found;
    }

    /** The last INFO answer for that bay, or null when none came yet. */
    public static HandheldInfoPayload infoFor(BlockPos pos) {
        return info != null && info.pos().equals(pos) ? info : null;
    }

    /** Asks the server about a bay when it is another bay than last time, or the answer is old, at most every 10 ticks. */
    public static void requestInfo(BlockPos pos, long now) {
        if (now - requestedAt < INFO_REQUEST_TICKS) {
            return;
        }
        if (pos.equals(requested) && now - requestedAt < INFO_REQUEST_TICKS * 2L) {
            return;
        }
        requested = pos;
        requestedAt = now;
        ClientPacketDistributor.sendToServer(new HandheldInfoRequest(pos));
    }
}
