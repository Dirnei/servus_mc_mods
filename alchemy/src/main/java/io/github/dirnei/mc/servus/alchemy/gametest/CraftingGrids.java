package io.github.dirnei.mc.servus.alchemy.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Crafting table checks for GameTests. */
final class CraftingGrids {

    private CraftingGrids() {
    }

    /** What a 3 wide grid of {@code rows} crafts, each character looked up in {@code key}; others are empty. */
    static ItemStack crafted(GameTestHelper helper, Map<Character, Item> key, String... rows) {
        List<ItemStack> grid = new ArrayList<>();
        for (String row : rows) {
            for (char c : row.toCharArray()) {
                Item item = key.get(c);
                grid.add(item == null ? ItemStack.EMPTY : new ItemStack(item));
            }
        }
        CraftingInput input = CraftingInput.of(3, rows.length, grid);
        return helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    /** Fails the test unless {@code result} is exactly one {@code expected}. */
    static void assertCrafts(GameTestHelper helper, ItemStack result, Item expected) {
        helper.assertTrue(result.is(expected) && result.getCount() == 1, "one " + expected + " expected, got " + result);
    }
}
