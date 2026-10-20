package io.github.dirnei.mc.servus.delicacies.cooking;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a vessel holds when it looks for a recipe: its input stacks and, for the Pot, its fluid. */
public record VesselInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
