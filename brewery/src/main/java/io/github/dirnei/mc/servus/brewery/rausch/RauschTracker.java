package io.github.dirnei.mc.servus.brewery.rausch;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Keeps each player's {@link Rausch} and shows it as status effects. Server only. The player data is the truth; the
 * effects follow it on every update, so whatever removes them (milk) is undone on the next one.
 */
public final class RauschTracker {

    /** Ticks between two updates of a player. */
    public static final int UPDATE_INTERVAL = 20;
    private static final int DAY_TICKS = 24000;
    /** How far an effect's remaining time may drift from the wanted one before it is replaced. */
    private static final int DRIFT = 2 * UPDATE_INTERVAL;

    private RauschTracker() {
    }

    /** The player's Rausch now. */
    public static Rausch get(Player player) {
        return player.getData(RauschAttachment.RAUSCH);
    }

    /** Sets the player's Rausch and shows it at once. */
    public static void set(Player player, Rausch rausch) {
        store(player, rausch);
        sync(player);
    }

    /** The day of the overworld clock; it turns at dawn and when players wake up from sleep. */
    public static long day(Player player) {
        return player.level().getOverworldClockTime() / DAY_TICKS;
    }

    /** A finished beer: raises the Rausch, or is the Hair of the Dog while hungover. */
    public static void drink(Player player) {
        RauschStats stats = stats();
        set(player, get(player).drink(stats.perDrink(), stats, day(player), player.level().getGameTime()));
    }

    /** {@code ticks} spent in the game on {@code day}: decay, the morning's Hangover, the effects and stumbling. */
    public static void advance(Player player, int ticks, long day) {
        RauschStats stats = stats();
        store(player, get(player).pass(ticks, stats).morning(day, player.level().getGameTime(), stats));
        sync(player);
        stumble(player);
    }

    /** Whether {@code instance} is the Nausea that the Rausch adds for the screen wobble. */
    public static boolean isRauschNausea(MobEffectInstance instance) {
        return instance.is(MobEffects.NAUSEA) && instance.isAmbient() && !instance.showIcon();
    }

    /** Brings the player's effects in line with their Rausch. */
    public static void sync(Player player) {
        RauschStats stats = stats();
        Rausch rausch = get(player);
        Optional<RauschLevel> level = rausch.level(stats);
        for (RauschLevel other : RauschLevel.values()) {
            if (level.map(wanted -> wanted != other).orElse(true)) {
                player.removeEffect(RauschEffects.effect(other));
            }
        }
        int duration = rausch.ticksAtLevel(stats) + UPDATE_INTERVAL;
        level.ifPresent(wanted -> show(player, RauschEffects.effect(wanted), duration, false, true));
        boolean wobble = level.map(wanted -> stats.level(wanted).wobble()).orElse(false);
        if (wobble) {
            MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
            if (nausea == null || isRauschNausea(nausea)) {
                show(player, MobEffects.NAUSEA, duration, true, false);
            }
        } else {
            MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
            if (nausea != null && isRauschNausea(nausea)) {
                player.removeEffect(MobEffects.NAUSEA);
            }
        }
        long gameTime = player.level().getGameTime();
        if (rausch.hungover(gameTime)) {
            show(player, RauschEffects.HANGOVER, rausch.hangoverLeft(gameTime), false, true);
        } else {
            player.removeEffect(RauschEffects.HANGOVER);
        }
    }

    private static void show(Player player, Holder<MobEffect> effect, int duration, boolean ambient, boolean icon) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && Math.abs(current.getDuration() - duration) <= DRIFT) {
            return;
        }
        if (current != null) {
            player.removeEffect(effect);
        }
        player.addEffect(new MobEffectInstance(effect, duration, 0, ambient, false, icon));
    }

    private static void stumble(Player player) {
        Optional<RauschEffect> effect = RauschEffects.active(player);
        if (effect.isEmpty() || !player.onGround() || player.isShiftKeyDown() || player.isPassenger() || player.isSpectator()) {
            return;
        }
        LevelStats stats = effect.get().stats();
        if (stats.stumbleStrength() <= 0 || player.getRandom().nextDouble() >= stats.stumbleChance()) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1, 0, 0);
        }
        Vec3 push = side.normalize().scale(stats.stumbleStrength() * (player.getRandom().nextBoolean() ? 1 : -1));
        player.push(push.x, 0, push.z);
        player.hurtMarked = true;
    }

    private static void store(Player player, Rausch rausch) {
        if (!rausch.equals(get(player))) {
            player.setData(RauschAttachment.RAUSCH, rausch);
        }
    }

    private static RauschStats stats() {
        return RauschStatsManager.INSTANCE.stats();
    }
}
