package io.github.dirnei.mc.servus.delicacies.board;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What a cut sees: the item on the board and the tool in the player's hand. */
public record CuttingInput(ItemStack item, ItemStack tool) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? item : tool;
    }

    @Override
    public int size() {
        return 2;
    }
}
