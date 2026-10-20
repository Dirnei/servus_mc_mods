package io.github.dirnei.mc.servus.delicacies.gametest;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlocks;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesEffects;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesItems;
import io.github.dirnei.mc.servus.delicacies.teller.MealBlock;
import io.github.dirnei.mc.servus.delicacies.teller.MealKind;
import io.github.dirnei.mc.servus.delicacies.teller.MealPortions;
import io.github.dirnei.mc.servus.delicacies.teller.PlateBlock;
import io.github.dirnei.mc.servus.delicacies.teller.ProperlyFullEffect;
import io.github.dirnei.mc.servus.foundation.gametest.ServusGameTest;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/**
 * The Teller: a placed meal eaten portion by portion, Properly Full after the last, the plate left behind, the
 * shipped meal recipes with their bowls, and the drops of full and started meals.
 */
public final class TellerGameTests {

    private static final BlockPos TABLE_SPOT = new BlockPos(2, 1, 2);

    private TellerGameTests() {
    }

    private static Player hungryPlayer(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        return player;
    }

    private static int portions(GameTestHelper helper) {
        return helper.getBlockState(TABLE_SPOT).getValue(MealBlock.PORTIONS);
    }

    private static ItemStack crafted(GameTestHelper helper, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return ServusRecipes.craftingFor(helper.getLevel(), input).map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static List<ItemStack> leftInGrid(GameTestHelper helper, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return ServusRecipes.craftingFor(helper.getLevel(), input).map(recipe -> List.copyOf(recipe.value().getRemainingItems(input)))
                .orElse(List.of()).stream().filter(stack -> !stack.isEmpty()).toList();
    }

    private static List<ItemStack> grid(Item... items) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = i < items.length ? new ItemStack(items[i]) : ItemStack.EMPTY;
        }
        return List.of(grid);
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void eatFourPortions(GameTestHelper helper) {
        helper.setBlock(TABLE_SPOT, ServusDelicaciesBlocks.meal(MealKind.ROAST_PORK_PLATE));
        Player player = hungryPlayer(helper);
        for (int eaten = 1; eaten <= MealPortions.FULL; eaten++) {
            player.getFoodData().setFoodLevel(0);
            InteractionResult result = KitchenTestSupport.click(helper, player, TABLE_SPOT, ItemStack.EMPTY);
            helper.assertTrue(result.consumesAction(), "portion " + eaten + " is eaten");
            helper.assertTrue(player.getFoodData().getFoodLevel() == 8, "a portion restores 8, got " + player.getFoodData().getFoodLevel());
            if (eaten < MealPortions.FULL) {
                helper.assertTrue(portions(helper) == MealPortions.FULL - eaten, "portions left after " + eaten);
                helper.assertFalse(player.hasEffect(ServusDelicaciesEffects.PROPERLY_FULL), "no buff before the last portion");
            }
        }
        helper.assertBlockPresent(ServusDelicaciesBlocks.PLATE.get(), TABLE_SPOT);
        MobEffectInstance buff = player.getEffect(ServusDelicaciesEffects.PROPERLY_FULL);
        helper.assertTrue(buff != null && buff.getDuration() == 1200, "Properly Full for 1200 ticks after the last portion, got " + buff);
        helper.assertTrue(KitchenTestSupport.click(helper, player, TABLE_SPOT, ItemStack.EMPTY) == InteractionResult.PASS,
                "the empty plate does nothing");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void fullPlayerEatsNothing(GameTestHelper helper) {
        helper.setBlock(TABLE_SPOT, ServusDelicaciesBlocks.meal(MealKind.SCHNITZEL_PLATE));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        KitchenTestSupport.click(helper, player, TABLE_SPOT, ItemStack.EMPTY);
        helper.assertTrue(portions(helper) == MealPortions.FULL, "a full player eats nothing");
        KitchenTestSupport.click(helper, player, TABLE_SPOT, new ItemStack(Items.STICK));
        helper.assertTrue(portions(helper) == MealPortions.FULL, "a stick eats nothing");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void schnitzelPortionFromData(GameTestHelper helper) {
        helper.setBlock(TABLE_SPOT, ServusDelicaciesBlocks.meal(MealKind.SCHNITZEL_PLATE));
        Player player = hungryPlayer(helper);
        KitchenTestSupport.click(helper, player, TABLE_SPOT, ItemStack.EMPTY);
        helper.assertTrue(player.getFoodData().getFoodLevel() == 5, "a Schnitzel portion restores 5, got " + player.getFoodData().getFoodLevel());
        helper.assertTrue(portions(helper) == 3, "three portions left");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void beerBreakfastFinishesWithoutBrewery(GameTestHelper helper) {
        helper.setBlock(TABLE_SPOT, ServusDelicaciesBlocks.meal(MealKind.WEISSWURST_BREAKFAST_WITH_WEISSBIER).defaultBlockState()
                .setValue(MealBlock.PORTIONS, 1));
        Player player = hungryPlayer(helper);
        KitchenTestSupport.click(helper, player, TABLE_SPOT, ItemStack.EMPTY);
        helper.assertBlockPresent(ServusDelicaciesBlocks.PLATE.get(), TABLE_SPOT);
        helper.assertTrue(player.hasEffect(ServusDelicaciesEffects.PROPERLY_FULL), "Properly Full after the breakfast");
        if (!ModList.get().isLoaded(ServusDelicaciesItems.BREWERY)) {
            helper.assertTrue(player.getInventory().isEmpty(), "without Brewery no beer is handed over");
        }
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void mealAssembly(GameTestHelper helper) {
        List<ItemStack> roast = grid(ServusDelicaciesItems.PLATE.get(), ServusDelicaciesItems.ROAST_PORK.get(),
                ServusDelicaciesItems.BREAD_DUMPLING.get(), ServusDelicaciesItems.POTATO_DUMPLING.get(),
                ServusDelicaciesItems.BRAISED_RED_CABBAGE.get(), ServusDelicaciesItems.GRAVY.get());
        helper.assertTrue(crafted(helper, roast).is(ServusDelicaciesItems.meal(MealKind.ROAST_PORK_PLATE)),
                "a Roast Pork Plate, got " + crafted(helper, roast));
        List<ItemStack> roastLeft = leftInGrid(helper, roast);
        helper.assertTrue(roastLeft.size() == 1 && roastLeft.getFirst().is(Items.BOWL), "the Gravy's Bowl stays, got " + roastLeft);

        List<ItemStack> schnitzel = grid(ServusDelicaciesItems.PLATE.get(), ServusDelicaciesItems.SCHNITZEL.get(),
                ServusDelicaciesItems.POTATO_SALAD.get());
        helper.assertTrue(crafted(helper, schnitzel).is(ServusDelicaciesItems.meal(MealKind.SCHNITZEL_PLATE)), "a Schnitzel Plate");
        List<ItemStack> schnitzelLeft = leftInGrid(helper, schnitzel);
        helper.assertTrue(schnitzelLeft.size() == 1 && schnitzelLeft.getFirst().is(Items.BOWL), "the salad's Bowl stays, got " + schnitzelLeft);

        List<ItemStack> breakfast = grid(ServusDelicaciesItems.PLATE.get(), ServusDelicaciesItems.WEISSWURST.get(),
                ServusDelicaciesItems.WEISSWURST.get(), ServusDelicaciesItems.PRETZEL.get(), ServusDelicaciesItems.SWEET_MUSTARD.get());
        helper.assertTrue(crafted(helper, breakfast).is(ServusDelicaciesItems.meal(MealKind.WEISSWURST_BREAKFAST)), "a Weisswurst Breakfast");

        List<ItemStack> plates = grid(Items.CLAY_BALL, Items.CLAY_BALL, Items.CLAY_BALL, Items.BLUE_DYE);
        ItemStack plate = crafted(helper, plates);
        helper.assertTrue(plate.is(ServusDelicaciesItems.PLATE.get()) && plate.getCount() == 2, "two Plates, got " + plate);

        boolean beerRecipe = ServusRecipes.byId(helper.getLevel(),
                Identifier.fromNamespaceAndPath(ServusDelicaciesIds.MOD_ID, "weisswurst_breakfast_with_weissbier")).isPresent();
        helper.assertTrue(beerRecipe == ModList.get().isLoaded(ServusDelicaciesItems.BREWERY),
                "the Weissbier breakfast recipe loads only with Brewery");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void mealDrops(GameTestHelper helper) {
        BlockState full = ServusDelicaciesBlocks.meal(MealKind.WEISSWURST_BREAKFAST).defaultBlockState();
        List<ItemStack> whole = Block.getDrops(full, helper.getLevel(), helper.absolutePos(TABLE_SPOT), null);
        helper.assertTrue(whole.size() == 1 && whole.getFirst().is(ServusDelicaciesItems.meal(MealKind.WEISSWURST_BREAKFAST)),
                "a full meal drops itself, got " + whole);
        List<ItemStack> started = Block.getDrops(full.setValue(MealBlock.PORTIONS, 2), helper.getLevel(), helper.absolutePos(TABLE_SPOT), null);
        helper.assertTrue(started.size() == 1 && started.getFirst().is(ServusDelicaciesItems.PLATE.get()) && started.getFirst().getCount() == 1,
                "a started meal drops only its Plate, got " + started);
        List<ItemStack> plate = Block.getDrops(ServusDelicaciesBlocks.PLATE.get().defaultBlockState(), helper.getLevel(),
                helper.absolutePos(TABLE_SPOT), null);
        helper.assertTrue(plate.size() == 1 && plate.getFirst().is(ServusDelicaciesItems.PLATE.get()), "a Plate drops itself, got " + plate);
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void plateNeedsASolidTop(GameTestHelper helper) {
        helper.setBlock(TABLE_SPOT.below(), Blocks.SMOOTH_STONE);
        helper.setBlock(TABLE_SPOT.above(), Blocks.AIR);
        helper.assertTrue(PlateBlock.restsOn(helper.getLevel(), helper.absolutePos(TABLE_SPOT)), "the floor carries a plate");
        helper.assertFalse(PlateBlock.restsOn(helper.getLevel(), helper.absolutePos(TABLE_SPOT.above(2))), "air carries no plate");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void properlyFullHeals(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(10);
        ProperlyFullEffect effect = ServusDelicaciesEffects.PROPERLY_FULL.get();
        helper.assertTrue(effect.shouldApplyEffectTickThisTick(ProperlyFullEffect.HEAL_INTERVAL, 0), "heals on its interval");
        helper.assertFalse(effect.shouldApplyEffectTickThisTick(ProperlyFullEffect.HEAL_INTERVAL / 2, 0), "not between heals");
        helper.assertTrue(effect.shouldApplyEffectTickThisTick(ProperlyFullEffect.HEAL_INTERVAL / 2, 1), "twice as often one level up");
        effect.applyEffectTick(helper.getLevel(), player, 0);
        helper.assertTrue(player.getHealth() == 11, "one health point back, got " + player.getHealth());
        helper.succeed();
    }
}
