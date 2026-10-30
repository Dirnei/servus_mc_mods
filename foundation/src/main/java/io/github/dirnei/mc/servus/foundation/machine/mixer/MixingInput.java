package io.github.dirnei.mc.servus.foundation.machine.mixer;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** What a Mixer holds when it looks for a recipe: its input stacks and its tank's fluid. */
public record MixingInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    /** Empty only without items and without fluid; the recipe lookup skips empty inputs. */
    @Override
    public boolean isEmpty() {
        return fluid.isEmpty() && RecipeInput.super.isEmpty();
    }
}
