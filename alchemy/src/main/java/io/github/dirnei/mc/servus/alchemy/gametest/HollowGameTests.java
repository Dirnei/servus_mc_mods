package io.github.dirnei.mc.servus.alchemy.gametest;

import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.Principles;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity;
import io.github.dirnei.mc.servus.alchemy.hollow.GnomeHollowBlockEntity.Binding;
import io.github.dirnei.mc.servus.alchemy.journal.Journal;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusFoundationCapabilities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
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

    /** A seeded iron vein, ripe or not, for a team that finished Villach. */
    static GnomeHollowBlockEntity ironHollow(GameTestHelper helper, boolean ripe) {
        UUID team = UUID.randomUUID();
        finishVillach(helper.getLevel(), team);
        GnomeHollowBlockEntity hollow = hollow(helper, team);
        hollow.seed(new ItemStack(Items.RAW_IRON));
        helper.assertTrue(hollow.vein().isPresent(), "raw iron should seed a vein");
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

    /** Raw iron seeds an iron vein by hand only under the sight and once the team finished Villach. */
    @ServusGameTest(timeoutTicks = 40)
    public static void seedingNeedsVillachAndSight(GameTestHelper helper) {
        UUID team = UUID.randomUUID();
        GnomeHollowBlockEntity hollow = hollow(helper, team);
        ItemStack sample = new ItemStack(Items.RAW_IRON, 2);
        Player player = player(helper, sample, true);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().isEmpty() && sample.getCount() == 2, "no iron before Villach");
        finishVillach(helper.getLevel(), team);
        player.removeAllEffects();
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().isEmpty() && sample.getCount() == 2, "without the sight the Hollow is stone");
        giveSight(player);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().isEmpty(), "cobblestone seeds nothing");
        player.setItemInHand(InteractionHand.MAIN_HAND, sample);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().map(id -> id.getPath().equals("iron")).orElse(false), "an iron vein should be seeded");
        helper.assertTrue(sample.getCount() == 1, "one sample is used up");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.RAW_COPPER));
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.vein().map(id -> id.getPath().equals("iron")).orElse(false), "one vein per hollow");
        helper.succeed();
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

    /** Only a player with Elemental Sight can bind a gnome, and only to a ripe vein. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bindingNeedsElementalSight(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, false);
        ItemStack crystals = new ItemStack(ServusAlchemyItems.SALT_CRYSTAL.get(), 3);
        Player player = player(helper, crystals, true);
        helper.assertTrue(hollow.bind(player) == Binding.NOT_RIPE, "an unripe vein takes no gnome");
        hollow.growth().ripeness = IRON_STEPS;
        player.removeAllEffects();
        helper.assertTrue(hollow.bind(player) == Binding.NO_SIGHT, "without the sight no gnome is bound");
        helper.useBlock(MACHINE, player);
        helper.assertTrue(!hollow.growth().bound && crystals.getCount() == 3, "a crystal used without the sight does nothing");
        giveSight(player);
        helper.useBlock(MACHINE, player);
        helper.assertTrue(hollow.growth().bound, "with the sight a gnome is bound");
        helper.assertTrue(crystals.getCount() == 2, "the crystal is used up");
        helper.assertTrue(hollow.bind(player) == Binding.ALREADY_BOUND, "one gnome per vein");
        helper.succeed();
    }

    /** A bound gnome digs raw iron into the store and pushes nothing into a chest beside it. */
    @ServusGameTest(timeoutTicks = 600)
    public static void boundHollowFillsItsStore(GameTestHelper helper) {
        BlockPos chestPos = MACHINE.east();
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = helper.getBlockEntity(chestPos, ChestBlockEntity.class);
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.growth().bound = true;
        helper.succeedWhen(() -> {
            helper.assertTrue(hollow.store().count() >= 2, "the store should get raw iron, has " + hollow.store().count());
            helper.assertTrue(chest.isEmpty(), "nothing is pushed into the chest");
        });
    }

    /** A full store stops the gnome. */
    @ServusGameTest(timeoutTicks = 2 * IRON_YIELD_TICKS)
    public static void fullStoreStops(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.growth().bound = true;
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

    /** Binding a gnome counts for Schwaz. */
    @ServusGameTest(timeoutTicks = 40)
    public static void bindingCountsForSchwaz(GameTestHelper helper) {
        Player player = player(helper, ItemStack.EMPTY, true);
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.bind(player);
        int count = Journal.journalOf(helper.getLevel().getServer(), hollow.team()).count("servus_alchemy:schwaz", "gnome");
        helper.assertTrue(count == 1, "the bound gnome should count for Schwaz, got " + count);
        helper.succeed();
    }

    /** A working Hollow offers no item, fluid, energy or production capability on any side. */
    @ServusGameTest(timeoutTicks = 40)
    public static void noCapabilities(GameTestHelper helper) {
        GnomeHollowBlockEntity hollow = ironHollow(helper, true);
        hollow.growth().bound = true;
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
        hollow.growth().bound = true;
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
}
