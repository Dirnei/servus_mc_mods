package io.github.dirnei.mc.servus.foundation.gametest;

import io.github.dirnei.mc.servus.foundation.filter.FilterDataMaps;
import io.github.dirnei.mc.servus.foundation.filter.FilterSpec;
import io.github.dirnei.mc.servus.foundation.filter.Filters;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.mill.MillingRecipe;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationComponents;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

/** The Carbon Filter chain, the filter data map and wearing filters through {@link Filters}. */
public final class FilterGameTests {

    private FilterGameTests() {
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void carbonFilterSpecLoads(GameTestHelper helper) {
        FilterSpec spec = FilterDataMaps.spec(fresh());
        helper.assertTrue(spec != null, "the Carbon Filter is a filter");
        helper.assertTrue(spec.points() == 512, "with 512 points, got " + spec.points());
        helper.assertTrue(spec.spent() == ServusFoundationItems.SPENT_FILTER.get(), "that turns into a Spent Filter");
        helper.assertTrue(Filters.pointsLeft(fresh()) == 512, "a fresh filter has every point left");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void wearSpendsPoints(GameTestHelper helper) {
        ItemStack filter = fresh();
        Filters.WearResult result = Filters.wear(filter, 40);
        helper.assertTrue(result.spent() == 40, "40 spent, got " + result.spent());
        helper.assertTrue(Filters.pointsLeft(result.stack()) == 472, "472 left, got " + Filters.pointsLeft(result.stack()));
        helper.assertTrue(result.stack().is(ServusFoundationItems.CARBON_FILTER.get()), "still a Carbon Filter");
        helper.assertFalse(filter.has(ServusFoundationComponents.FILTER_USED.get()), "the input stack is not changed");
        helper.assertTrue(Filters.pointsLeft(filter) == 512, "the input keeps every point");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void lastPointsGiveASpentFilter(GameTestHelper helper) {
        ItemStack filter = fresh();
        filter.set(ServusFoundationComponents.FILTER_USED.get(), 482);
        Filters.WearResult result = Filters.wear(filter, 40);
        helper.assertTrue(result.spent() == 30, "only the 30 left are spent, got " + result.spent());
        helper.assertTrue(result.stack().is(ServusFoundationItems.SPENT_FILTER.get()) && result.stack().getCount() == 1,
                "one Spent Filter, got " + result.stack());
        helper.assertTrue(filter.is(ServusFoundationItems.CARBON_FILTER.get()) && Filters.pointsLeft(filter) == 30,
                "the input stack is not changed");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void paperIsNoFilter(GameTestHelper helper) {
        ItemStack paper = new ItemStack(Items.PAPER, 5);
        Filters.WearResult result = Filters.wear(paper, 10);
        helper.assertTrue(result.spent() == 0, "nothing spent");
        helper.assertTrue(result.stack().is(Items.PAPER) && result.stack().getCount() == 5, "the Paper is unchanged");
        helper.assertFalse(Filters.isFilter(paper), "Paper is no filter");
        helper.assertTrue(Filters.wear(ItemStack.EMPTY, 10).spent() == 0, "an empty stack spends nothing");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void aStackOfSeveralDoesNotWear(GameTestHelper helper) {
        ItemStack filters = new ItemStack(ServusFoundationItems.CARBON_FILTER.get(), 3);
        Filters.WearResult result = Filters.wear(filters, 10);
        helper.assertTrue(result.spent() == 0, "nothing spent");
        helper.assertTrue(result.stack().getCount() == 3 && !result.stack().has(ServusFoundationComponents.FILTER_USED.get()),
                "the stack is unchanged");
        helper.assertTrue(Filters.isFilter(filters), "it still is a filter");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void freshFiltersStackAndUsedOnesDoNot(GameTestHelper helper) {
        ItemStack used = Filters.wear(fresh(), 1).stack();
        helper.assertTrue(ItemStack.isSameItemSameComponents(fresh(), fresh()), "fresh filters stack");
        helper.assertFalse(ItemStack.isSameItemSameComponents(fresh(), used), "a used filter does not stack with fresh ones");
        helper.assertTrue(fresh().getMaxStackSize() == 16, "filters stack to 16");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void charcoalChainRecipes(GameTestHelper helper) {
        Optional<RecipeHolder<MillingRecipe>> milling = ServusRecipes.first(helper.getLevel(), MachineContent.MILLING.get(),
                new SingleRecipeInput(new ItemStack(Items.CHARCOAL)));
        helper.assertTrue(milling.isPresent() && milling.get().value().result().is(ServusFoundationItems.CHARCOAL_POWDER.get()),
                "the Mill grinds Charcoal into Charcoal Powder");
        helper.assertTrue(ServusRecipes.first(helper.getLevel(), MachineContent.MILLING.get(),
                new SingleRecipeInput(new ItemStack(Items.COAL))).isEmpty(), "Coal has no milling recipe");
        SingleRecipeInput powder = new SingleRecipeInput(new ItemStack(ServusFoundationItems.CHARCOAL_POWDER.get()));
        ItemStack blasted = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.BLASTING, powder, helper.getLevel())
                .map(holder -> holder.value().assemble(powder))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(blasted.is(ServusFoundationItems.ACTIVATED_CHARCOAL.get()), "blasting makes Activated Charcoal, got " + blasted);
        helper.assertTrue(helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING, powder, helper.getLevel()).isEmpty(),
                "a plain furnace does not smelt it");
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void carbonFilterCraftsWithWheat(GameTestHelper helper) {
        CraftingInput input = CraftingInput.of(2, 2, List.of(
                new ItemStack(ServusFoundationItems.ACTIVATED_CHARCOAL.get()), new ItemStack(ServusFoundationItems.ACTIVATED_CHARCOAL.get()),
                new ItemStack(Items.WHEAT), new ItemStack(Items.PAPER)));
        ItemStack result = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
        helper.assertTrue(result.is(ServusFoundationItems.CARBON_FILTER.get()) && result.getCount() == 1,
                "the recipe makes one Carbon Filter, got " + result);
        helper.succeed();
    }

    @ServusGameTest(timeoutTicks = 20)
    public static void spentFilterComposts(GameTestHelper helper) {
        Compostable compostable = ServusFoundationItems.SPENT_FILTER.get().builtInRegistryHolder().getData(NeoForgeDataMaps.COMPOSTABLES);
        helper.assertTrue(compostable != null && compostable.chance() == 0.65F, "the Spent Filter composts at 0.65, got " + compostable);
        helper.succeed();
    }

    private static ItemStack fresh() {
        return new ItemStack(ServusFoundationItems.CARBON_FILTER.get());
    }
}
