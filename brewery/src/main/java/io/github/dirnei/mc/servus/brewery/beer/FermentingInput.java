package io.github.dirnei.mc.servus.brewery.beer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a Fermenting Vat holds when it looks for a recipe: its yeast and its tank. */
public record FermentingInput(ItemStack yeast, FluidStack wort) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return yeast;
    }

    @Override
    public int size() {
        return 1;
    }
}
