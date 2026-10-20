package io.github.dirnei.mc.servus.brewery.gametest;

import io.github.dirnei.mc.servus.brewery.crop.HopPoleBlock;
import io.github.dirnei.mc.servus.brewery.crop.HopStage;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryBlocks;
import io.github.dirnei.mc.servus.brewery.registry.ServusBreweryItems;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Barley grows and drops like wheat; hops are planted on a pole, climb it, bear cones and are picked. */
public final class BreweryCropGameTests {

    private static final BlockPos GROUND = new BlockPos(2, 0, 2);
    private static final BlockPos FOOT = GROUND.above();
    private static final int GRASS_BREAKS = 300;

    private BreweryCropGameTests() {
    }

    // ---------- barley ----------

    @ServusGameTest
    public static void barleyIsPlantedFromSeeds(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.BARLEY_SEEDS.get(), 2));
        useOnTop(helper, player, GROUND);
        helper.assertBlockPresent(ServusBreweryBlocks.BARLEY.get(), FOOT);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "planting should use one seed");
        helper.succeed();
    }

    @ServusGameTest
    public static void barleyRipensWithBoneMealAndDropsBarley(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        CropBlock barley = ServusBreweryBlocks.BARLEY.get();
        helper.setBlock(FOOT, barley.getStateForAge(0));
        BlockPos absolute = helper.absolutePos(FOOT);
        for (int i = 0; i < 20 && !barley.isMaxAge(helper.getLevel().getBlockState(absolute)); i++) {
            BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), absolute, null);
        }
        BlockState ripe = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(barley.isMaxAge(ripe), "bone meal should ripen the barley");
        List<ItemStack> drops = Block.getDrops(ripe, helper.getLevel(), absolute, null);
        helper.assertTrue(count(drops, ServusBreweryItems.BARLEY.get()) == 1, "ripe barley should drop one Barley, got " + drops);
        helper.assertTrue(count(drops, ServusBreweryItems.BARLEY_SEEDS.get()) >= 1, "ripe barley should drop seeds, got " + drops);
        helper.succeed();
    }

    @ServusGameTest
    public static void youngBarleyDropsOnlyASeed(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        helper.setBlock(FOOT, ServusBreweryBlocks.BARLEY.get().getStateForAge(3));
        BlockPos absolute = helper.absolutePos(FOOT);
        List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(absolute), helper.getLevel(), absolute, null);
        helper.assertTrue(count(drops, ServusBreweryItems.BARLEY.get()) == 0, "young barley should drop no Barley, got " + drops);
        helper.assertTrue(count(drops, ServusBreweryItems.BARLEY_SEEDS.get()) == 1, "young barley should drop one seed, got " + drops);
        helper.succeed();
    }

    @ServusGameTest
    public static void shortGrassDropsBarleySeedsOnlyWithoutShears(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.GRASS_BLOCK);
        helper.setBlock(FOOT, Blocks.SHORT_GRASS);
        BlockPos absolute = helper.absolutePos(FOOT);
        BlockState grass = helper.getLevel().getBlockState(absolute);
        int byHand = 0;
        int withShears = 0;
        for (int i = 0; i < GRASS_BREAKS; i++) {
            byHand += count(Block.getDrops(grass, helper.getLevel(), absolute, null, null, ItemStack.EMPTY), ServusBreweryItems.BARLEY_SEEDS.get());
            withShears += count(Block.getDrops(grass, helper.getLevel(), absolute, null, null, new ItemStack(Items.SHEARS)),
                    ServusBreweryItems.BARLEY_SEEDS.get());
        }
        helper.assertTrue(byHand > 0, "short grass broken by hand should sometimes drop Barley Seeds");
        helper.assertTrue(withShears == 0, "short grass cut with shears should drop no Barley Seeds");
        helper.succeed();
    }

    // ---------- hop pole ----------

    @ServusGameTest
    public static void aFourthPoleIsRefused(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        helper.setBlock(FOOT, ServusBreweryBlocks.HOP_POLE.get());
        helper.setBlock(FOOT.above(), ServusBreweryBlocks.HOP_POLE.get());
        Player player = holding(helper, new ItemStack(ServusBreweryItems.HOP_POLE.get(), 4));
        useOnTop(helper, player, FOOT.above());
        helper.assertBlockPresent(ServusBreweryBlocks.HOP_POLE.get(), FOOT.above(2));
        useOnTop(helper, player, FOOT.above(2));
        helper.assertBlockPresent(Blocks.AIR, FOOT.above(3));
        helper.assertTrue(player.getMainHandItem().getCount() == 3, "only the third pole should be placed");
        helper.succeed();
    }

    @ServusGameTest
    public static void aRhizomePlantsAtTheFootOfTheColumn(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        column(helper, HopStage.BARE, HopStage.BARE, HopStage.BARE);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.HOP_RHIZOME.get(), 2));
        helper.useBlock(FOOT.above(2), player);
        BlockState foot = helper.getBlockState(FOOT);
        helper.assertTrue(HopPoleBlock.stageOf(foot) == HopStage.YOUNG && foot.getValue(HopPoleBlock.ROOTED),
                "the foot pole should hold a young rooted vine, got " + foot);
        helper.assertTrue(HopPoleBlock.stageOf(helper.getBlockState(FOOT.above(2))) == HopStage.BARE, "the clicked top pole stays bare");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "planting should use one rhizome");
        helper.succeed();
    }

    @ServusGameTest
    public static void aRhizomeDoesNotGrowOnStone(GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        column(helper, HopStage.BARE, HopStage.BARE);
        Player player = holding(helper, new ItemStack(ServusBreweryItems.HOP_RHIZOME.get(), 1));
        helper.useBlock(FOOT, player);
        helper.assertTrue(HopPoleBlock.stageOf(helper.getBlockState(FOOT)) == HopStage.BARE, "no vine on a pole standing on stone");
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the rhizome should stay in the hand");
        helper.succeed();
    }

    @ServusGameTest
    public static void boneMealMakesTheVineClimbAndBearCones(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        column(helper, HopStage.YOUNG, HopStage.BARE, HopStage.BARE);
        BlockPos top = helper.absolutePos(FOOT.above(2));
        boneMeal(helper, top);
        boneMeal(helper, top);
        assertColumn(helper, "after two steps", HopStage.GROWN, HopStage.YOUNG, HopStage.BARE);
        for (int i = 0; i < 5; i++) {
            boneMeal(helper, top);
        }
        assertColumn(helper, "after seven steps", HopStage.GROWN, HopStage.CONES, HopStage.CONES);
        helper.assertFalse(BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), top, null),
                "a fully grown column should refuse bone meal");
        helper.succeed();
    }

    @ServusGameTest
    public static void randomTicksMakeTheVineClimb(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        column(helper, HopStage.GROWN, HopStage.BARE);
        for (int i = 0; i < 400 && HopPoleBlock.stageOf(helper.getBlockState(FOOT.above())) == HopStage.BARE; i++) {
            helper.randomTick(FOOT);
        }
        helper.assertTrue(HopPoleBlock.stageOf(helper.getBlockState(FOOT.above())) == HopStage.YOUNG,
                "random ticks on the foot should let the vine climb onto the pole above");
        helper.succeed();
    }

    @ServusGameTest
    public static void pickingConesGivesHopsAndKeepsTheVine(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        column(helper, HopStage.GROWN, HopStage.CONES);
        helper.useBlock(FOOT.above(), helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertItemEntityPresent(ServusBreweryItems.HOPS.get(), FOOT.above(), 3);
        assertColumn(helper, "after picking", HopStage.GROWN, HopStage.GROWN);
        helper.succeed();
    }

    @ServusGameTest
    public static void breakingTheFootDropsTheRhizome(GameTestHelper helper) {
        moistFarmland(helper, GROUND);
        column(helper, HopStage.GROWN, HopStage.GROWN, HopStage.CONES);
        helper.getLevel().destroyBlock(helper.absolutePos(FOOT), true);
        for (int part = 0; part < 3; part++) {
            helper.assertBlockPresent(Blocks.AIR, FOOT.above(part));
        }
        helper.assertItemEntityCountIs(ServusBreweryItems.HOP_RHIZOME.get(), FOOT, 4, 1);
        helper.assertItemEntityCountIs(ServusBreweryItems.HOP_POLE.get(), FOOT, 4, 3);
        helper.assertItemEntityPresent(ServusBreweryItems.HOPS.get(), FOOT.above(2), 4);
        helper.succeed();
    }

    // ---------- fixtures ----------

    private static void moistFarmland(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
    }

    /** A column of hop poles on {@link #FOOT} with these stages from the foot up; a foot with a vine is rooted. */
    private static void column(GameTestHelper helper, HopStage... stages) {
        BlockState pole = ServusBreweryBlocks.HOP_POLE.get().defaultBlockState();
        for (int part = 0; part < stages.length; part++) {
            BlockState state = HopPoleBlock.withStage(pole, stages[part]).setValue(HopPoleBlock.ROOTED, part == 0 && stages[0].hasVine());
            helper.setBlock(FOOT.above(part), state);
        }
    }

    private static void assertColumn(GameTestHelper helper, String when, HopStage... expected) {
        for (int part = 0; part < expected.length; part++) {
            HopStage actual = HopPoleBlock.stageOf(helper.getBlockState(FOOT.above(part)));
            helper.assertTrue(actual == expected[part], when + ": part " + part + " should be " + expected[part] + ", was " + actual);
        }
    }

    private static void boneMeal(GameTestHelper helper, BlockPos absolute) {
        helper.assertTrue(BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), helper.getLevel(), absolute, null),
                "bone meal should make a growth step");
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    /** Uses the held item on the top face of the block at {@code pos}, as a right click there does. */
    private static void useOnTop(GameTestHelper helper, Player player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        helper.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(absolute).add(0, 0.5, 0), Direction.UP, absolute, false));
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
