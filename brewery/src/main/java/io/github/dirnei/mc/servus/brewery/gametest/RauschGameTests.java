package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.rausch.Rausch;
import io.github.dirnei.mc.servus.brewery.rausch.RauschEffects;
import io.github.dirnei.mc.servus.brewery.rausch.RauschLevel;
import io.github.dirnei.mc.servus.brewery.rausch.RauschStats;
import io.github.dirnei.mc.servus.brewery.rausch.RauschStatsManager;
import io.github.dirnei.mc.servus.brewery.rausch.RauschTracker;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** The Rausch: drinking raises it, time lowers it, its effects, the knockback, the Hangover and the Hair of the Dog. */
public final class RauschGameTests {

    private static final int PUFFERFISH_NAUSEA = 300;

    private RauschGameTests() {
    }

    @ServusGameTest
    public static void oneBeerMakesTipsy(GameTestHelper helper) {
        Player player = drinker(helper, 1);
        assertOnly(helper, player, RauschLevel.TIPSY);
        assertNear(helper, 1, player.getAttributeValue(Attributes.LUCK), "luck when Tipsy");
        assertNear(helper, 2, player.getAttributeValue(Attributes.ARMOR), "armor when Tipsy");
        int duration = player.getEffect(RauschEffects.TIPSY).getDuration();
        helper.assertTrue(Math.abs(duration - stats().decayTicks()) <= 2 * RauschTracker.UPDATE_INTERVAL,
                "Tipsy should last about one decay period, lasts " + duration);
        helper.assertTrue(player.getEffect(MobEffects.NAUSEA) == null, "Tipsy should not wobble");
        helper.succeed();
    }

    @ServusGameTest
    public static void fourBeersMakeDrunk(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        assertOnly(helper, player, RauschLevel.DRUNK);
        assertNear(helper, 0.5, player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), "fall damage when Drunk");
        assertNear(helper, 6, player.getAttributeValue(Attributes.ARMOR), "armor when Drunk");
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(nausea != null && RauschTracker.isRauschNausea(nausea), "Drunk should wobble with an icon-less Nausea");
        helper.succeed();
    }

    @ServusGameTest
    public static void fiveBeersMakePlastered(GameTestHelper helper) {
        Player player = drinker(helper, 5);
        assertOnly(helper, player, RauschLevel.PLASTERED);
        assertNear(helper, 0, player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), "fall damage when Plastered");
        assertNear(helper, 3, player.getAttributeValue(Attributes.LUCK), "luck when Plastered");
        helper.succeed();
    }

    @ServusGameTest
    public static void decayLowersTheLevel(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        long day = RauschTracker.day(player);
        RauschTracker.advance(player, stats().decayTicks(), day);
        assertOnly(helper, player, RauschLevel.MERRY);
        helper.assertTrue(player.getEffect(MobEffects.NAUSEA) == null, "Merry should not wobble");
        assertNear(helper, 1, player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER), "fall damage when Merry");
        RauschTracker.advance(player, stats().decayTicks() * 10, day);
        helper.assertTrue(RauschEffects.active(player).isEmpty(), "the Rausch should wear off");
        assertNear(helper, 0, player.getAttributeValue(Attributes.LUCK), "luck when sober");
        assertNear(helper, 0, player.getAttributeValue(Attributes.ARMOR), "armor when sober");
        helper.succeed();
    }

    @ServusGameTest
    public static void milkDoesNotSoberUp(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        player.removeAllEffects();
        RauschTracker.advance(player, 0, RauschTracker.day(player));
        assertOnly(helper, player, RauschLevel.DRUNK);
        helper.succeed();
    }

    @ServusGameTest
    public static void drunkKnockbackIsLarger(GameTestHelper helper) {
        Player sober = drinker(helper, 0);
        Player drunk = drinker(helper, 4);
        Player tipsy = drinker(helper, 1);
        double soberPush = knockedBack(sober);
        assertNear(helper, 1.5, knockedBack(drunk) / soberPush, "Drunk knockback factor");
        assertNear(helper, 1, knockedBack(tipsy) / soberPush, "Tipsy knockback factor");
        helper.succeed();
    }

    @ServusGameTest
    public static void hangoverNextDay(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        long day = RauschTracker.day(player);
        RauschTracker.advance(player, 0, day);
        helper.assertTrue(!player.hasEffect(RauschEffects.HANGOVER), "no Hangover on the same day");
        RauschTracker.advance(player, 0, day + 1);
        helper.assertTrue(player.hasEffect(RauschEffects.HANGOVER), "a Hangover the next day");
        helper.assertTrue(RauschTracker.get(player).points() == 0, "the Hangover clears the Rausch");
        helper.assertTrue(RauschEffects.active(player).isEmpty(), "no level while hungover");
        assertNear(helper, 1 + stats().hangover().miningSpeed(), player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED),
                "mining speed when hungover");
        player.removeAllEffects();
        RauschTracker.advance(player, 0, day + 1);
        helper.assertTrue(player.hasEffect(RauschEffects.HANGOVER), "milk should not end the Hangover");
        helper.succeed();
    }

    @ServusGameTest
    public static void noHangoverWhenMerry(GameTestHelper helper) {
        Player player = drinker(helper, 3);
        RauschTracker.advance(player, 0, RauschTracker.day(player) + 1);
        helper.assertTrue(!player.hasEffect(RauschEffects.HANGOVER), "Merry should not bring a Hangover");
        assertOnly(helper, player, RauschLevel.MERRY);
        helper.succeed();
    }

    @ServusGameTest
    public static void hairOfTheDogCuresHangover(GameTestHelper helper) {
        Player player = drinker(helper, 5);
        RauschTracker.advance(player, 0, RauschTracker.day(player) + 1);
        helper.assertTrue(player.hasEffect(RauschEffects.HANGOVER), "a Hangover after Plastered");
        drink(helper, player, ServusBreweryItems.WEISSBIER.get());
        helper.assertTrue(!player.hasEffect(RauschEffects.HANGOVER), "the Hair of the Dog should end the Hangover");
        assertOnly(helper, player, RauschLevel.TIPSY);
        Rausch rausch = RauschTracker.get(player);
        helper.assertTrue(rausch.points() == stats().hangover().hairOfTheDog(), "the Hair of the Dog sets the Rausch, got " + rausch);
        assertNear(helper, 1, player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED), "mining speed after the cure");
        helper.succeed();
    }

    @ServusGameTest
    public static void foreignNauseaKeepsItsOwnDuration(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, PUFFERFISH_NAUSEA));
        assertForeignNausea(helper, player, "after the pufferfish");
        RauschTracker.advance(player, 0, RauschTracker.day(player));
        assertForeignNausea(helper, player, "on the next update while Drunk");
        RauschTracker.advance(player, stats().decayTicks() * 10, RauschTracker.day(player));
        helper.assertTrue(RauschEffects.active(player).isEmpty(), "the Rausch should wear off");
        assertForeignNausea(helper, player, "after sobering up");
        helper.succeed();
    }

    @ServusGameTest
    public static void rauschNauseaReturnsAfterForeignNausea(GameTestHelper helper) {
        Player player = drinker(helper, 4);
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, PUFFERFISH_NAUSEA));
        player.removeEffect(MobEffects.NAUSEA);
        RauschTracker.advance(player, 0, RauschTracker.day(player));
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(nausea != null && RauschTracker.isRauschNausea(nausea), "the Rausch should wobble again, got " + nausea);
        helper.succeed();
    }

    private static void assertForeignNausea(GameTestHelper helper, Player player, String when) {
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(nausea != null && !RauschTracker.isRauschNausea(nausea) && nausea.getDuration() == PUFFERFISH_NAUSEA,
                "the pufferfish's Nausea should last its own " + PUFFERFISH_NAUSEA + " ticks " + when + ", got " + nausea);
    }

    private static RauschStats stats() {
        return RauschStatsManager.INSTANCE.stats();
    }

    private static Player drinker(GameTestHelper helper, int beers) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < beers; i++) {
            drink(helper, player, ServusBreweryItems.HELLES.get());
        }
        return player;
    }

    private static void drink(GameTestHelper helper, Player player, Item beer) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(beer));
        player.getMainHandItem().finishUsingItem(helper.getLevel(), player);
    }

    private static void assertOnly(GameTestHelper helper, Player player, RauschLevel wanted) {
        for (RauschLevel level : RauschLevel.values()) {
            Holder<MobEffect> effect = RauschEffects.effect(level);
            helper.assertTrue(player.hasEffect(effect) == (level == wanted),
                    "expected only " + wanted + ", " + level + " is " + (player.hasEffect(effect) ? "on" : "off"));
        }
        helper.assertTrue(RauschEffects.active(player).map(effect -> effect.level()).equals(Optional.of(wanted)),
                "the active level should be " + wanted);
    }

    /** The horizontal speed a fixed knockback gives the player from standing still. */
    private static double knockedBack(Player player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.knockback(0.4, 1, 0);
        return player.getDeltaMovement().horizontalDistance();
    }

    private static void assertNear(GameTestHelper helper, double expected, double actual, String what) {
        helper.assertTrue(Math.abs(expected - actual) < 1.0E-3, what + ": expected " + expected + ", got " + actual);
    }
}
