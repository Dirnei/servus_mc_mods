package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.terminal.MostStocked;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** The 3 by 3 crafting grid of the pattern editor: matching it to a recipe and laying a recipe out in it. */
public final class CraftingGrid {

    public static final int GRID = 9;

    private CraftingGrid() {
    }

    /** The vanilla crafting recipe a 3 by 3 grid matches, for saving and for the editor's preview. */
    public static Optional<RecipeHolder<CraftingRecipe>> match(Level level, List<ItemStack> cells) {
        if (cells.stream().allMatch(ItemStack::isEmpty)) {
            return Optional.empty();
        }
        return ServusRecipes.craftingFor(level, CraftingInput.of(3, 3, cells));
    }

    /** "2 Oak Planks, 1 Coal" for a list of stacks with counts. */
    public static MutableComponent stacks(List<ItemStack> stacks) {
        MutableComponent text = Component.empty();
        for (int i = 0; i < stacks.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(stacks.get(i).getCount() + " ").append(stacks.get(i).getHoverName());
        }
        return text;
    }

    /**
     * The template laid out by a vanilla crafting recipe for the recipe viewer:
     * per cell the stack {@link #stockedChoice} picks from what its ingredient accepts.
     *
     * @param stock the warehouse's requestable stock, largest first; empty without a warehouse
     */
    public static Optional<List<ItemStack>> templateOf(Level level, Identifier id, List<Map.Entry<ItemKey, Long>> stock) {
        Optional<RecipeHolder<?>> holder = ServusRecipes.byId(level, id);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
            return Optional.empty();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(ItemStack.EMPTY);
        }
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<Optional<Ingredient>> ingredients = ServusRecipes.gridIngredients(recipe);
        for (int i = 0; i < ingredients.size() && i < GRID; i++) {
            ItemStack item = ingredients.get(i).map(ingredient -> stockedChoice(ServusRecipes.items(ingredient), stock))
                    .orElse(ItemStack.EMPTY);
            if (!item.isEmpty()) {
                int cell = width == 3 ? i : (i / width) * 3 + i % width;
                if (cell < GRID) {
                    cells.set(cell, item);
                }
            }
        }
        return Optional.of(cells);
    }

    /**
     * A single stack for a cell that accepts these items: the stocked stack of
     * an accepted item the warehouse has the most of, with its components,
     * else the first accepted item. Empty when nothing is accepted.
     */
    public static ItemStack stockedChoice(List<ItemStack> accepted, List<Map.Entry<ItemKey, Long>> stock) {
        Set<Item> items = new HashSet<>();
        ItemStack first = ItemStack.EMPTY;
        for (ItemStack option : accepted) {
            if (!option.isEmpty()) {
                items.add(option.getItem());
                if (first.isEmpty()) {
                    first = option;
                }
            }
        }
        if (first.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack fallback = first.copyWithCount(1);
        return MostStocked.pick(stock, key -> items.contains(key.item())).map(key -> key.stack(1)).orElse(fallback);
    }
}
