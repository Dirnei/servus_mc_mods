package io.github.dirnei.mc.servus.shuttles.integration.jei;

import io.github.dirnei.mc.servus.shuttles.pattern.CraftingGrid;
import java.util.List;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;

/** The stacks JEI shows in the cells of a crafting recipe, for the terminal transfer payloads. */
final class ShownCells {

    private ShownCells() {
    }

    /**
     * Per cell, row by row, at most {@link CraftingGrid#MAX_SHOWN} of the stacks
     * JEI shows. JEI's crafting category lays out nine input slots in grid order.
     */
    static List<List<ItemStack>> of(IRecipeSlotsView slots) {
        return slots.getSlotViews(RecipeIngredientRole.INPUT).stream()
                .limit(CraftingGrid.GRID)
                .map(slot -> slot.getItemStacks().limit(CraftingGrid.MAX_SHOWN).map(ItemStack::copy).toList())
                .toList();
    }
}
