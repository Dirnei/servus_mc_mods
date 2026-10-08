package io.github.dirnei.mc.servus.storage.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
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
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, cells), level);
    }

    /** The template laid out by a vanilla crafting recipe, one ingredient item per cell, for the recipe viewer. */
    public static Optional<List<ItemStack>> templateOf(Level level, ResourceLocation id) {
        Optional<RecipeHolder<?>> holder = level.getRecipeManager().byKey(id);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe recipe)) {
            return Optional.empty();
        }
        List<ItemStack> cells = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            cells.add(ItemStack.EMPTY);
        }
        int width = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < GRID; i++) {
            ItemStack[] items = ingredients.get(i).getItems();
            if (items.length > 0) {
                int cell = width == 3 ? i : (i / width) * 3 + i % width;
                if (cell < GRID) {
                    cells.set(cell, items[0].copyWithCount(1));
                }
            }
        }
        return Optional.of(cells);
    }
}
