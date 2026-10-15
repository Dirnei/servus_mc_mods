package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hint.AncientHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnome;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeData;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeLedger.State;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeSavedData;
import io.github.dirnei.mc.servus.alchemy.hint.OldGnomeSettings;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The hints at hidden things: the old gnome waking while a team mines deepslate, his traces and finding him. */
public final class HintGameTests {

    /** The deepslate block at the end of the test tunnel, the one the player breaks. */
    private static final BlockPos FACE = new BlockPos(6, 2, 6);
    /** Another deepslate block close to the tunnel end. */
    private static final BlockPos ABOVE_FACE = new BlockPos(6, 3, 6);
    private static final String OLD_GNOME_CHAPTER = "servus_alchemy:old_gnome";
    /** Always wakes, at any height. */
    private static final OldGnomeSettings CERTAIN = OldGnomeSettings.DEFAULTS.withWake(1, Integer.MAX_VALUE).withTraceChance(0);

    private HintGameTests() {
    }

    /** Fills the test area with deepslate and digs a two high tunnel along x at z 6 that ends at {@link #FACE}. */
    private static void tunnel(GameTestHelper helper) {
        for (int x = 0; x < 12; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = 0; z < 12; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.DEEPSLATE);
                }
            }
        }
        for (int x = 0; x < FACE.getX(); x++) {
            helper.setBlock(new BlockPos(x, 2, 6), Blocks.AIR);
            helper.setBlock(new BlockPos(x, 3, 6), Blocks.AIR);
        }
    }

    /** A team that finished Villach and Schwaz. */
    private static UUID afterSchwaz(ServerLevel level) {
        return afterSchwaz(level, UUID.randomUUID());
    }

    private static UUID afterSchwaz(ServerLevel level, UUID team) {
        HollowGameTests.finishVillach(level, team);
        HollowGameTests.finishSchwaz(level, team);
        return team;
    }

    private static Optional<BlockPos> breakAt(GameTestHelper helper, UUID team, BlockPos relative, OldGnomeSettings settings) {
        BlockPos pos = helper.absolutePos(relative);
        ServerLevel level = helper.getLevel();
        return OldGnome.onBroken(level, team, pos, level.getBlockState(pos), settings, RandomSource.create(42));
    }

    private static AncientHollowBlockEntity woken(GameTestHelper helper, UUID team) {
        Optional<BlockPos> woken = breakAt(helper, team, FACE, CERTAIN);
        helper.assertTrue(woken.isPresent(), "the old gnome should wake");
        if (!(helper.getLevel().getBlockEntity(woken.get()) instanceof AncientHollowBlockEntity hollow)) {
            throw helper.assertionException(Component.literal("an Ancient Hollow should stand at " + woken.get()));
        }
        return hollow;
    }

    private static State state(GameTestHelper helper, UUID team) {
        return OldGnomeSavedData.get(helper.getLevel().getServer()).ledger().entry(team).state();
    }

    private static int ancientHollows(GameTestHelper helper) {
        int count = 0;
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y <= 5; y++) {
                for (int z = 0; z < 12; z++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).is(ServusAlchemyBlocks.ANCIENT_HOLLOW.get())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static List<ItemEntity> tracesNear(GameTestHelper helper, Vec3 at, double radius) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(radius),
                item -> item.getItem().is(OldGnome.TRACES));
    }

    /** After Schwaz, a broken deepslate block with chance 1 turns a tunnel wall into the Ancient Hollow, open to the tunnel. */
    @ServusGameTest(timeoutTicks = 20)
    public static void wakesAfterTheChapter(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        AncientHollowBlockEntity hollow = woken(helper, team);
        BlockPos pos = hollow.getBlockPos();
        helper.assertTrue(pos.distSqr(helper.absolutePos(FACE)) <= 25, "the Hollow should be within a few blocks, got " + pos);
        Direction face = hollow.getBlockState().getValue(AncientHollowBlock.FACING);
        helper.assertTrue(helper.getLevel().getBlockState(pos.relative(face)).isAir(), "the open face should look into the tunnel");
        helper.assertTrue(team.equals(hollow.team()), "the Hollow belongs to the team");
        helper.assertTrue(state(helper, team) == State.AWAKE, "the ledger knows him awake");
        helper.succeed();
    }

    /** Before Schwaz is finished nothing wakes, even with chance 1. */
    @ServusGameTest(timeoutTicks = 20)
    public static void notBeforeTheChapter(GameTestHelper helper) {
        tunnel(helper);
        UUID team = UUID.randomUUID();
        HollowGameTests.finishVillach(helper.getLevel(), team);
        helper.assertTrue(breakAt(helper, team, FACE, CERTAIN).isEmpty(), "nothing wakes before Schwaz");
        helper.assertTrue(ancientHollows(helper) == 0, "no Ancient Hollow appears");
        helper.assertTrue(state(helper, team) == State.NONE, "the team's gnome still sleeps");
        helper.succeed();
    }

    /** Deepslate at or above the configured height wakes nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void notAboveTheHeight(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        int faceY = helper.absolutePos(FACE).getY();
        helper.assertTrue(breakAt(helper, team, FACE, CERTAIN.withWake(1, faceY)).isEmpty(), "nothing wakes at the height limit");
        helper.assertTrue(ancientHollows(helper) == 0, "no Ancient Hollow appears");
        helper.succeed();
    }

    /** A team has one old gnome: further breaks wake no second Hollow. */
    @ServusGameTest(timeoutTicks = 20)
    public static void onlyOncePerTeam(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        woken(helper, team);
        helper.assertTrue(breakAt(helper, team, ABOVE_FACE, CERTAIN).isEmpty(), "a second break wakes nothing");
        helper.assertTrue(ancientHollows(helper) == 1, "exactly one Ancient Hollow, got " + ancientHollows(helper));
        helper.succeed();
    }

    /** Broken before he was found, the Hollow is lost and a later break wakes him again. */
    @ServusGameTest(timeoutTicks = 20)
    public static void lostBeforeFoundWakesAgain(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        AncientHollowBlockEntity hollow = woken(helper, team);
        helper.getLevel().destroyBlock(hollow.getBlockPos(), false);
        helper.assertTrue(state(helper, team) == State.NONE, "the lost Hollow lets him sleep again");
        helper.assertTrue(breakAt(helper, team, ABOVE_FACE, CERTAIN).isPresent(), "a later break wakes him again");
        helper.succeed();
    }

    /** Once found he never wakes again, even when his Hollow is broken. */
    @ServusGameTest(timeoutTicks = 20)
    public static void foundNeverWakesAgain(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        AncientHollowBlockEntity hollow = woken(helper, team);
        OldGnome.find(helper.getLevel(), team, hollow);
        helper.assertTrue(hollow.found() && state(helper, team) == State.FOUND, "he is found");
        helper.getLevel().destroyBlock(hollow.getBlockPos(), false);
        helper.assertTrue(state(helper, team) == State.FOUND, "found stays found");
        helper.assertTrue(breakAt(helper, team, ABOVE_FACE, CERTAIN).isEmpty(), "nothing wakes again");
        helper.succeed();
    }

    /** When he wakes, one trace lies in front of his open face. */
    @ServusGameTest(timeoutTicks = 20)
    public static void traceLiesInFrontWhenHeWakes(GameTestHelper helper) {
        tunnel(helper);
        AncientHollowBlockEntity hollow = woken(helper, afterSchwaz(helper.getLevel()));
        List<ItemEntity> traces = tracesNear(helper, hollow.inFront(0.3), 1);
        helper.assertTrue(traces.size() == 1, "one trace should lie in front of the Hollow, got " + traces.size());
        helper.succeed();
    }

    /** With trace chance 1, deepslate broken near him drops a trace; a stranger's break does not. */
    @ServusGameTest(timeoutTicks = 20)
    public static void traceDropsNearHim(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        woken(helper, team);
        Vec3 at = helper.absolutePos(ABOVE_FACE).getCenter();
        breakAt(helper, UUID.randomUUID(), ABOVE_FACE, CERTAIN.withTraceChance(1));
        helper.assertTrue(tracesNear(helper, at, 0.6).isEmpty(), "another team's break drops no trace");
        breakAt(helper, team, ABOVE_FACE, CERTAIN.withTraceChance(1));
        helper.assertTrue(tracesNear(helper, at, 0.6).size() == 1, "the team's break near him drops a trace");
        helper.succeed();
    }

    /** Picking up a trace counts the find for the player's team in The Old Gnome. */
    @ServusGameTest(timeoutTicks = 20)
    public static void tracePickupCountsFind(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        UUID team = afterSchwaz(helper.getLevel(), OldGnome.teamOf(player));
        OldGnome.onTracePickedUp(helper.getLevel(), player, new ItemStack(ServusAlchemyItems.RED_FELT_SCRAP.get()));
        helper.assertTrue(Journal.journalOf(helper.getLevel().getServer(), team).count(OLD_GNOME_CHAPTER, "trace") == 1,
                "the trace counts as found");
        helper.succeed();
    }

    /** Uses the journal on the Hollow's open face; returns whether the click was used. */
    private static boolean useJournal(GameTestHelper helper, Player player, AncientHollowBlockEntity hollow) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ServusAlchemyItems.PARACELSUS_JOURNAL.get()));
        BlockPos pos = hollow.getBlockPos();
        BlockState state = hollow.getBlockState();
        BlockHitResult hit = new BlockHitResult(pos.getCenter(), state.getValue(AncientHollowBlock.FACING), pos, false);
        return state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit).consumesAction();
    }

    /** Under the sight the journal finds him: the find counts for the finder, the Hollow and the ledger know him found. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalUnderSightFindsHim(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        AncientHollowBlockEntity hollow = woken(helper, team);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(ElementalSight.EFFECT, 200));
        UUID finder = afterSchwaz(helper.getLevel(), OldGnome.teamOf(player));
        helper.assertTrue(useJournal(helper, player, hollow), "the journal is used on him");
        helper.assertTrue(hollow.found() && state(helper, team) == State.FOUND, "he is found");
        helper.assertTrue(Journal.journalOf(helper.getLevel().getServer(), finder).count(OLD_GNOME_CHAPTER, "gnome") == 1,
                "the find counts for the finder's team");
        helper.succeed();
    }

    /** Without the sight the journal does nothing to him. */
    @ServusGameTest(timeoutTicks = 20)
    public static void journalWithoutSightDoesNothing(GameTestHelper helper) {
        tunnel(helper);
        UUID team = afterSchwaz(helper.getLevel());
        AncientHollowBlockEntity hollow = woken(helper, team);
        helper.assertFalse(useJournal(helper, helper.makeMockPlayer(GameType.SURVIVAL), hollow), "the click passes");
        helper.assertTrue(!hollow.found() && state(helper, team) == State.AWAKE, "he is not found without the sight");
        helper.succeed();
    }

    /** Finding a trace and the old gnome finishes The Old Gnome; it waits for Schwaz. */
    @ServusGameTest(timeoutTicks = 20)
    public static void oldGnomeChapterFinishes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        UUID early = UUID.randomUUID();
        Journal.recordItem(level, early, OldGnome.FIND, new ItemStack(ServusAlchemyItems.TINY_PICKAXE.get()), 1);
        helper.assertTrue(Journal.journalOf(level.getServer(), early).count(OLD_GNOME_CHAPTER, "trace") == 0, "the chapter waits for Schwaz");
        UUID team = afterSchwaz(level);
        Journal.recordItem(level, team, OldGnome.FIND, new ItemStack(ServusAlchemyItems.SALT_OFFERING.get()), 1);
        helper.assertFalse(Journal.isFinished(level.getServer(), team, OLD_GNOME_CHAPTER), "a trace alone is not enough");
        Journal.recordNamed(level, team, OldGnome.FIND, OldGnome.OLD_GNOME);
        helper.assertTrue(Journal.isFinished(level.getServer(), team, OLD_GNOME_CHAPTER), "The Old Gnome is finished");
        helper.succeed();
    }

    /** With a pickaxe the Ancient Hollow drops Cobbled Deepslate, like Deepslate. */
    @ServusGameTest(timeoutTicks = 20)
    public static void breaksIntoCobbledDeepslate(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 1, 5));
        helper.getLevel().setBlock(pos, ServusAlchemyBlocks.ANCIENT_HOLLOW.get().defaultBlockState(), Block.UPDATE_ALL);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                helper.getLevel().getBlockEntity(pos), null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.COBBLED_DEEPSLATE), "one Cobbled Deepslate, got " + drops);
        helper.succeed();
    }

    /** The shipped settings: rare, after Schwaz, below Y 0. */
    @ServusGameTest(timeoutTicks = 20)
    public static void shippedSettings(GameTestHelper helper) {
        OldGnomeSettings settings = OldGnomeData.INSTANCE.settings();
        helper.assertTrue(settings.equals(OldGnomeSettings.DEFAULTS), "the shipped file matches the defaults, got " + settings);
        helper.assertTrue(settings.wakeChance() > 0 && settings.wakeChance() < 0.01, "waking is rare");
        helper.assertTrue(settings.wakeChapter().equals("servus_alchemy:schwaz") && settings.maxY() == 0, "after Schwaz, below Y 0");
        helper.succeed();
    }
}
