package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlock;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Binding;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Gift;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Seeding;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinGrowth;
import io.github.dirnei.mc.servus.alchemy.hollow.VeinSeedItem;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.gametest.GameTestSteps;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;

/** The Gnome Hollow: tended by hand under Elemental Sight, stone to everyone else. */
public final class HollowGameTests {

    private static final BlockPos MACHINE = AlchemyGameTests.MACHINE;
    private static final int IRON_STEPS = 20;
    private static final int IRON_YIELD_TICKS = 200;

    private HollowGameTests() {
    }

    static GnomeHollowBlockEntity hollow(GameTestHelper helper, UUID team) {
        helper.setBlock(MACHINE, ServusAlchemyBlocks.GNOME_HOLLOW.get().defaultBlockState());
        GnomeHollowBlockEntity hollow = helper.getBlockEntity(MACHINE, GnomeHollowBlockEntity.class);
        hollow.setTeam(team);
        return hollow;
    }

    static void finishVillach(ServerLevel level, UUID team) {
        Journal.recordItem(level, team, "calcine", new ItemStack(Items.COBBLESTONE), 100);
        Journal.recordPrinciple(level, team, Principle.SALT, 1000);
        Journal.recordEvent(level, team, "purify", 100);
        Journal.recordItem(level, team, "coagulate", new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get()), 1);
    }

    /** Finishes Schwaz through its events; Villach must be finished first. */
    static void finishSchwaz(ServerLevel level, UUID team) {
        Journal.recordAthanor(level, team, new ItemStack(Items.EXPERIENCE_BOTTLE), "albedo");
        Journal.recordEvent(level, team, "sight", 1);
        Journal.recordEvent(level, team, "bind", 1);
    }

    /** A Seed of the shipped vein {@code path}. */
    static ItemStack seed(String path) {
        return VeinSeedItem.of(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, path));
    }

    /** A Hollow at {@link #MACHINE} with a gnome in it, for a fresh team. */
    static GnomeHollowBlockEntity boundHollow(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = hollow(helper, UUID.randomUUID());
        hollow.growth().bound = true;
        return hollow;
    }

    /** A Hollow with a gnome and the shipped vein {@code path} planted at ripeness 0. */
    static GnomeHollowBlockEntity plantedHollow(GameTestHelper helper, String path) {
        GnomeHollowBlockEntity hollow = boundHollow(helper);
        helper.assertTrue(hollow.seed(seed(path)) == Seeding.PLANTED, "the Seed of " + path + " should be planted");
        return hollow;
    }

    /** An iron vein with its gnome, ripe or not. */
    static GnomeHollowBlockEntity ironHollow(GameTestHelper helper, boolean ripe) {
        GnomeHollowBlockEntity hollow = plantedHollow(helper, "iron");
        if (ripe) {
            hollow.growth().ripeness = IRON_STEPS;
        }
        return hollow;
    }

    /** A survival mock player with {@code stack} in the main hand, with or without Elemental Sight. */
    static Player player(GameTestHelper helper, ItemStack stack, boolean sight) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        if (sight) {
            giveSight(player);
        }
        return player;
    }

    static void giveSight(Player player) {
        player.addEffect(new MobEffectInstance(ElementalSight.EFFECT, 200));
    }

    /** A Salt Crystal calls a gnome into an empty Hollow under the sight; no vein is needed and one gnome is enough. */
    @ServusGameTest(timeoutTicks = 40)
    public static void saltCrystalCallsTheGnomeFirst(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = hollow(helper, UUID.randomUUID());
        ItemStack crystals = new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get(), 3);
        Player player = player(helper, crystals, false);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(!hollow.growth().bound && crystals.getCount() == 3, "without the sight the crystal does nothing");
        helper.assertTrue(hollow.bind(player) == Binding.NO_SIGHT, "without the sight no gnome is bound");
        giveSight(player);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.growth().bound && hollow.vein().isEmpty(), "a gnome moves into the empty Hollow");
        helper.assertTrue(crystals.getCount() == 2, "the crystal is used up");
        helper.useBlock(MACHINE, player);
        helper.assertTrue(crystals.getCount() == 2, "a second crystal stays in the hand");
        helper.assertTrue(hollow.bind(player) == Binding.ALREADY_BOUND, "one gnome per Hollow");
        helper.succeed();
    }

    /** Under the sight the gnome plants a Seed of Iron, whatever chapter the team reached; one Seed is used up. */
    @ServusGameTest(timeoutTicks = 40)
    public static void gnomePlantsASeed(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = boundHollow(helper);
        ItemStack seeds = seed("iron").copyWithCount(2);
        Player player = player(helper, seeds, false);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().isEmpty() && seeds.getCount() == 2, "without the sight the Hollow is stone");
        giveSight(player);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().map(id -> id.getPath().equals("iron")).orElse(false), "an iron vein should be planted");
        helper.assertTrue(hollow.growth().ripeness == 0, "the vein starts at ripeness 0");
        helper.assertTrue(seeds.getCount() == 1, "one Seed is used up");
        helper.useBlock(MACHINE, player);
        helper.assertTrue(seeds.getCount() == 1, "a Seed of the vein that grows stays in the hand");
        helper.succeed();
    }

    /** Raw ore and ore blocks plant nothing, even with a gnome: only Seeds do. */
    @ServusGameTest(timeoutTicks = 40)
    public static void rawOreIsRefused(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = boundHollow(helper);
        for (ItemStack sample : List.of(new ItemStack(Items.RAW_GOLD), new ItemStack(Items.GOLD_ORE), new ItemStack(Items.DIAMOND))) {
            Player player = player(helper, sample, true);
            helper.useBlock(MACHINE, player);
            helper.assertTrue(hollow.vein().isEmpty() && sample.getCount() == 1, sample.getItem() + " should plant nothing");
            helper.assertTrue(hollow.seed(sample) == Seeding.NOT_A_SEED, sample.getItem() + " is no Seed");
        }
        helper.succeed();
    }

    /** A Hollow without a gnome takes no Seed. */
    @ServusGameTest(timeoutTicks = 40)
    public static void seedNeedsAGnome(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = hollow(helper, UUID.randomUUID());
        ItemStack seed = seed("iron");
        helper.useBlock(MACHINE, player(helper, seed, true));
        helper.assertTrue(hollow.vein().isEmpty() && seed.getCount() == 1, "no gnome, nothing planted");
        helper.assertTrue(hollow.seed(seed) == Seeding.NO_GNOME, "the Hollow needs a gnome first");
        helper.succeed();
    }

    /** A Seed of a vein that is not loaded plants nothing. */
    @ServusGameTest(timeoutTicks = 20)
    public static void unknownSeedPlantsNothing(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = boundHollow(helper);
        ItemStack seed = VeinSeedItem.of(Identifier.fromNamespaceAndPath("servus_test", "tin"));
        helper.assertTrue(hollow.seed(seed) == Seeding.UNKNOWN_SEED && hollow.vein().isEmpty(), "an unknown vein plants nothing");
        helper.succeed();
    }

    /** Every shipped vein is planted by its own Seed. */
    @ServusGameTest(timeoutTicks = 20)
    public static void everyShippedSeedPlants(GameTestHelper helper) {
        List<String> veins = List.of("iron", "copper", "gold", "redstone", "diamond");
        for (int i = 0; i < veins.size(); i++) {
            BlockPos pos = MACHINE.east(i);
            helper.setBlock(pos, ServusAlchemyBlocks.GNOME_HOLLOW.get().defaultBlockState());
            GnomeHollowBlockEntity hollow = helper.getBlockEntity(pos, GnomeHollowBlockEntity.class);
            hollow.growth().bound = true;
            String vein = veins.get(i);
            helper.assertTrue(hollow.seed(seed(vein)) == Seeding.PLANTED, "the Seed of " + vein + " should be planted");
            helper.assertTrue(hollow.vein().map(id -> id.getPath().equals(vein)).orElse(false), "the vein is " + vein);
        }
        helper.succeed();
    }

    /** A Seed of another kind makes the gnome dig out the ripe iron vein and plant gold; the store keeps its iron. */
    @ServusGameTest(timeoutTicks = 40)
    public static void anotherSeedSwapsTheVein(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.store().load(5);
        ItemStack gold = seed("gold");
        helper.useBlock(MACHINE, player(helper, gold, true));
        helper.assertTrue(gold.isEmpty(), "the Seed of Gold is used up");
        helper.assertTrue(hollow.vein().map(id -> id.getPath().equals("gold")).orElse(false), "the Hollow grows gold now");
        helper.assertTrue(hollow.growth().ripeness == 0 && hollow.growth().bound, "the gold vein starts at 0 and the gnome stays");
        List<ItemStack> taken = hollow.takeStore();
        helper.assertTrue(taken.size() == 1 && taken.getFirst().is(Items.RAW_IRON) && taken.getFirst().getCount() == 5,
                "the store keeps the 5 Raw Iron, got " + taken);
        helper.succeed();
    }

    /** After a swap the gnome digs nothing into a store that still holds the old vein's ore. */
    @ServusGameTest(timeoutTicks = 100)
    public static void storeOfTheOldVeinWaits(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.growth().yieldTimer = IRON_YIELD_TICKS - 1;
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(hollow.store().count() == 1, "the iron gnome dug one Raw Iron");
            hollow.seed(seed("gold"));
            int steps = hollow.veinDef().orElseThrow().growth().steps();
            hollow.growth().ripeness = steps;
            hollow.growth().yieldTimer = hollow.veinDef().orElseThrow().growth().yieldTicks() - 1;
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(hollow.store().count() == 1, "no gold joins the Raw Iron, has " + hollow.store().count());
                helper.assertTrue(hollow.takeStore().getFirst().is(Items.RAW_IRON), "the Raw Iron comes out");
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(hollow.store().count() == 1 && hollow.storedYield().is(Items.RAW_GOLD),
                            "with the store empty the gnome digs gold");
                    helper.succeed();
                });
            });
        });
    }

    /** A planted vein without a gnome, from before gnomes came first, does not ripen. */
    @ServusGameTest(timeoutTicks = 300)
    public static void noRipeningWithoutAGnome(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, false);
        hollow.growth().bound = false;
        hollow.tanks().insert(new Principles(150, 0, 30));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(hollow.growth().ripeness == 0, "nobody tends it, so it stays at 0");
            helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 150, "its Salt stays in the tank");
            helper.succeed();
        });
    }

    /** A gnome bound to a ripe diamond vein digs diamonds into its store. */
    @ServusGameTest(timeoutTicks = 100)
    public static void diamondGnomeYieldsDiamonds(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = plantedHollow(helper, "diamond");
        int steps = hollow.veinDef().orElseThrow().growth().steps();
        int yieldTicks = hollow.veinDef().orElseThrow().growth().yieldTicks();
        hollow.growth().ripeness = steps;
        hollow.growth().yieldTimer = yieldTicks - 20;
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.store().count() == 1, "the store should hold a diamond, has " + hollow.store().count());
            helper.assertTrue(hollow.takeStore().getFirst().is(Items.DIAMOND), "the store holds diamonds");
        });
    }

    /** One ripening step takes its principles and waits for more. */
    @ServusGameTest(timeoutTicks = 300)
    public static void ripeningTakesPrinciples(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, false);
        hollow.tanks().insert(new Principles(150, 0, 30));
        helper.runAfterDelay(250, () -> {
            helper.assertTrue(hollow.growth().ripeness == 1, "one step should have grown, got " + hollow.growth().ripeness);
            helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 50, "50 mB Salt should remain, got " + hollow.tanks().amount(Principle.SALT));
            helper.assertTrue(hollow.tanks().amount(Principle.MERCURY) == 10, "10 mB Mercury should remain");
            helper.succeed();
        });
    }

    /** A Salt bucket pours into the Hollow only under the sight, and the empty bucket comes back. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bucketNeedsSight(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, false);
        Player player = player(helper, new ItemStack(PrincipleFluids.entry(Principle.SALT).bucket().get()), false);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 0, "without the sight the bucket pours nothing in");
        giveSight(player);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.tanks().amount(Principle.SALT) == 1000, "with the sight 1,000 mB Salt, got " + hollow.tanks().amount(Principle.SALT));
        helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the empty bucket comes back");
        helper.succeed();
    }

    /** A bound gnome digs raw iron into the store and pushes nothing into a chest beside it. */
    @ServusGameTest(timeoutTicks = 600)
    public static void boundHollowFillsItsStore(GameTestHelper helper) {
        BlockPos chestPos = MACHINE.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.store().count() >= 2, "the store should get raw iron, has " + hollow.store().count());
            helper.assertTrue(chest.isEmpty(), "nothing is pushed into the chest");
        });
    }

    /** A full store stops the gnome. */
    @ServusGameTest(timeoutTicks = 2 * IRON_YIELD_TICKS)
    public static void fullStoreStops(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.store().load(64);
        helper.runAfterDelay(IRON_YIELD_TICKS + 20, () -> {
            helper.assertTrue(hollow.store().count() == 64, "the store stays at 64, has " + hollow.store().count());
            helper.assertTrue(hollow.growth().yieldTimer == 0, "the gnome waits while the store is full");
            helper.succeed();
        });
    }

    /** An empty hand under the sight takes the whole store; without the sight it takes nothing. */
    @ServusGameTest(timeoutTicks = 40)
    public static void emptyHandTakesTheStore(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.store().load(70);
        Player player = player(helper, ItemStack.EMPTY, false);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.store().count() == 70, "without the sight nothing is taken");
        giveSight(player);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.store().isEmpty(), "the store is empty");
        int taken = player.getInventory().countItem(Items.RAW_IRON);
        helper.assertTrue(taken == 70, "the player holds 70 raw iron, got " + taken);
        helper.succeed();
    }

    /** Binding a gnome counts for Schwaz, with no vein planted yet. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bindingCountsForSchwaz(GameTestHelper helper) {
        Player player = player(helper, ItemStack.EMPTY, true);
        UUID team = UUID.randomUUID();
        finishVillach(helper.getLevel(), team);
        GnomeHollowBlockEntity hollow = hollow(helper, team);
        hollow.bind(player);
        int count = Journal.journalOf(helper.getLevel().getServer(), hollow.team()).count("servus_alchemy:schwaz", "gnome");
        helper.assertTrue(count == 1, "the bound gnome should count for Schwaz, got " + count);
        helper.succeed();
    }

    /** The last ripening step of a gold vein counts for Wandering years' gold task; earlier steps do not. */
    @ServusGameTest(timeoutTicks = 100)
    public static void ripeGoldVeinCountsForWanderingYears(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        finishVillach(helper.getLevel(), team);
        finishSchwaz(helper.getLevel(), team);
        GnomeHollowBlockEntity hollow = plantedHollow(helper, "gold");
        hollow.setTeam(team);
        VeinGrowth.Vein gold = hollow.veinDef().orElseThrow().growth();
        hollow.tanks().insert(gold.stepCost().plus(gold.stepCost()));
        hollow.growth().ripeness = gold.steps() - 2;
        hollow.growth().stepTimer = gold.stepTicks() - 1;
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(hollow.growth().ripeness == gold.steps() - 1, "one step short of ripe");
            helper.assertTrue(goldTask(helper, team) == 0, "an unripe vein counts nothing");
            hollow.growth().stepTimer = gold.stepTicks() - 1;
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(hollow.growth().ripe(gold), "the gold vein should be ripe");
                helper.assertTrue(goldTask(helper, team) == 1, "the ripe gold vein counts, got " + goldTask(helper, team));
                helper.succeed();
            });
        });
    }

    private static int goldTask(GameTestHelper helper, UUID team) {
        return Journal.journalOf(helper.getLevel().getServer(), team).count(AlchemyGameTests.WANDERING_YEARS, "gold");
    }

    /** A working Hollow offers no item, fluid, energy or production capability on any side. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noCapabilities(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        BlockPos pos = helper.absolutePos(MACHINE);
        ServerLevel level = helper.getLevel();
        List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (Direction side : sides) {
            helper.assertTrue(level.getCapability(Capabilities.Item.BLOCK, pos, side) == null, "no items toward " + side);
            helper.assertTrue(level.getCapability(Capabilities.Fluid.BLOCK, pos, side) == null, "no fluids toward " + side);
            helper.assertTrue(level.getCapability(Capabilities.Energy.BLOCK, pos, side) == null, "no energy toward " + side);
            helper.assertTrue(level.getCapability(ServusFoundationCapabilities.PRODUCTION, pos, side) == null, "no production toward " + side);
        }
        helper.succeed();
    }

    private static List<ItemStack> drops(GameTestHelper helper, GnomeHollowBlockEntity hollow, Player player, ItemStack tool) {
        return Block.getDrops(hollow.getBlockState(), helper.getLevel(), helper.absolutePos(MACHINE), hollow, player, tool);
    }

    private static boolean dropsExactly(List<ItemStack> drops, Item item) {
        return drops.size() == 1 && drops.get(0).is(item) && drops.get(0).getCount() == 1;
    }

    /** Without the sight a Hollow breaks like stone into Cobblestone. */
    @ServusGameTest(timeoutTicks = 40)
    public static void breakWithoutSightDropsCobblestone(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.store().load(5);
        List<ItemStack> drops = drops(helper, hollow, player(helper, ItemStack.EMPTY, false), new ItemStack(Items.STONE_PICKAXE));
        helper.assertTrue(dropsExactly(drops, Items.COBBLESTONE), "one Cobblestone should drop, got " + drops);
        helper.succeed();
    }

    /** Without the sight Silk Touch gives Stone. */
    @ServusGameTest(timeoutTicks = 40)
    public static void silkTouchWithoutSightDropsStone(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        List<ItemStack> drops = drops(helper, hollow, player(helper, ItemStack.EMPTY, false), tool);
        helper.assertTrue(dropsExactly(drops, Items.STONE), "one Stone should drop, got " + drops);
        helper.succeed();
    }

    /** Under the sight a fresh, empty Hollow drops. */
    @ServusGameTest(timeoutTicks = 40)
    public static void breakWithSightDropsAFreshHollow(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        List<ItemStack> drops = drops(helper, hollow, player(helper, ItemStack.EMPTY, true), new ItemStack(Items.STONE_PICKAXE));
        helper.assertTrue(dropsExactly(drops, ServusAlchemyItems.GNOME_HOLLOW.get()), "one Gnome Hollow should drop, got " + drops);
        helper.assertTrue(drops.get(0).getComponentsPatch().isEmpty(), "the dropped Hollow carries nothing");
        helper.succeed();
    }

    /** Broken under the sight, the store comes out beside the fresh Hollow. */
    @ServusGameTest(timeoutTicks = 40)
    public static void breakWithSightDropsTheStore(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.store().load(5);
        BlockState state = hollow.getBlockState();
        Player player = player(helper, ItemStack.EMPTY, true);
        helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
        state.getBlock().playerDestroy(helper.getLevel(), player, helper.absolutePos(MACHINE), state, hollow, new ItemStack(Items.STONE_PICKAXE));
        helper.assertItemEntityCountIs(Items.RAW_IRON, MACHINE, 2, 5);
        helper.assertItemEntityCountIs(ServusAlchemyItems.GNOME_HOLLOW.get(), MACHINE, 2, 1);
        helper.succeed();
    }

    /** A bound, working gnome under its base rate. */
    static GnomeHollowBlockEntity workingHollow(GameTestHelper helper) {
        return ironHollow(helper, true);
    }

    /** A cookie given by hand under the sight is used up and makes the gnome dig twice as fast. */
    @ServusGameTest(timeoutTicks = 3 * IRON_YIELD_TICKS)
    public static void likedGiftSpeedsUpTheGnome(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = workingHollow(helper);
        ItemStack cookies = new ItemStack(Items.COOKIE, 3);
        helper.useBlock(MACHINE, player(helper, cookies, true));
        helper.assertTrue(cookies.getCount() == 2, "one cookie is used up, " + cookies.getCount() + " left");
        helper.assertTrue(hollow.boostFactor() == 2 && hollow.boostTicksLeft() == 12000,
                "a cookie boosts x2 for 12,000 ticks, got x" + hollow.boostFactor() + " for " + hollow.boostTicksLeft());
        helper.runAfterDelay(2 * IRON_YIELD_TICKS + 10, () -> {
            helper.assertTrue(hollow.store().count() == 4, "twice the base rate of 2 raw iron, got " + hollow.store().count());
            helper.succeed();
        });
    }

    /** Under the sight a stick on a bound Hollow stays in the hand and starts no boost. */
    @ServusGameTest(timeoutTicks = 40)
    public static void dislikedItemIsRefused(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = workingHollow(helper);
        ItemStack stick = new ItemStack(Items.STICK);
        helper.useBlock(MACHINE, player(helper, stick, true));
        helper.assertTrue(stick.getCount() == 1, "the stick stays in the hand");
        helper.assertTrue(hollow.boostTicksLeft() == 0, "no boost from a stick");
        helper.assertTrue(hollow.offerGift(new ItemStack(Items.RAW_IRON)) == Gift.DISLIKED, "raw iron is no gift");
        helper.succeed();
    }

    /** Without the sight a cookie does nothing to the Hollow. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noGiftWithoutSight(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = workingHollow(helper);
        ItemStack cookie = new ItemStack(Items.COOKIE);
        helper.useBlock(MACHINE, player(helper, cookie, false));
        helper.assertTrue(cookie.getCount() == 1, "the cookie stays in the hand");
        helper.assertTrue(hollow.boostTicksLeft() == 0, "no boost without the sight");
        helper.succeed();
    }

    /** A Hollow without a gnome takes no gift; a block offering one never loses its item. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noGiftWithoutGnome(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.growth().bound = false;
        ItemStack cookie = new ItemStack(Items.COOKIE);
        helper.useBlock(MACHINE, player(helper, cookie, true));
        helper.assertTrue(cookie.getCount() == 1 && hollow.boostTicksLeft() == 0, "no gnome, no gift");
        hollow.growth().bound = true;
        helper.assertTrue(hollow.offerGift(cookie) == Gift.TAKEN, "a bound gnome takes the cookie");
        helper.assertTrue(cookie.getCount() == 1, "offerGift leaves the stack to the caller");
        helper.succeed();
    }

    /** A boost runs out and the gnome digs at the base rate again. */
    @ServusGameTest(timeoutTicks = 100)
    public static void boostExpires(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = workingHollow(helper);
        hollow.boost().give(2, 20);
        int[] timer = new int[1];
        helper.startSequence()
                .thenExecuteAfter(30, () -> {
                    helper.assertTrue(hollow.boostTicksLeft() == 0 && hollow.boostFactor() == 1, "the boost should have run out");
                    timer[0] = hollow.growth().yieldTimer;
                })
                .thenExecuteAfter(10, () -> helper.assertTrue(hollow.growth().yieldTimer == timer[0] + 10,
                        "base rate: 10 ticks of work in 10 ticks, got " + (hollow.growth().yieldTimer - timer[0])))
                .thenSucceed();
    }

    /** The boost's factor and time left are saved with the Hollow. */
    @ServusGameTest(timeoutTicks = 40)
    public static void boostIsSaved(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = workingHollow(helper);
        hollow.boost().give(2, 5000);
        CompoundTag saved = hollow.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.setBlock(MACHINE, Blocks.AIR.defaultBlockState());
        GnomeHollowBlockEntity fresh = hollow(helper, UUID.randomUUID());
        fresh.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(fresh.boostFactor() == 2 && fresh.boostTicksLeft() == 5000,
                "x2 for 5,000 ticks should be loaded, got x" + fresh.boostFactor() + " for " + fresh.boostTicksLeft());
        helper.succeed();
    }

    /** A placed Hollow faces the player who placed it, from whichever side he stands. */
    @ServusGameTest(timeoutTicks = 40)
    public static void placedHollowFacesThePlacer(GameTestHelper helper) {
        helper.setBlock(MACHINE.below(), Blocks.STONE);
        assertPlacedFacing(helper, Direction.SOUTH);
        helper.setBlock(MACHINE, Blocks.AIR);
        assertPlacedFacing(helper, Direction.EAST);
        helper.succeed();
    }

    /** Places a Hollow on {@link #MACHINE} as a player standing on its {@code side}, looking at it. */
    private static void assertPlacedFacing(GameTestHelper helper, Direction side) {
        Player player = player(helper, ItemStack.EMPTY, false);
        Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(MACHINE.relative(side, 2)));
        player.snapTo(feet.x, feet.y, feet.z, side.getOpposite().toYRot(), 0);
        GameTestSteps.useOn(helper, player, new ItemStack(ServusAlchemyItems.GNOME_HOLLOW.get()), MACHINE.below(), Direction.UP);
        helper.assertBlockProperty(MACHINE, GnomeHollowBlock.FACING, side);
    }
}
