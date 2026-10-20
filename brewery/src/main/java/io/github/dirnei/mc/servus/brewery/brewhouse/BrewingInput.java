package io.github.dirnei.mc.servus.brewery.brewhouse;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What the Brew Kettle holds when it looks for a recipe: its input stacks and its water tank. */
public record BrewingInput(List<ItemStack> items, FluidStack water) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
