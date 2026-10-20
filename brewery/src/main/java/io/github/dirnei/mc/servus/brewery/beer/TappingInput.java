package io.github.dirnei.mc.servus.brewery.beer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** An empty glass held to a Cask's tap, and the beer in the Cask. */
public record TappingInput(ItemStack glass, FluidStack beer) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return glass;
    }

    @Override
    public int size() {
        return 1;
    }
}
