package io.github.dirnei.mc.servus.manufacturing.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** The input slots of an assembler as seen by recipes. */
public record AssemblerRecipeInput(List<ItemStack> stacks) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
