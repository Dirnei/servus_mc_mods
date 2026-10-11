package io.github.dirnei.mc.servus.foundation.logistics.facade;

import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsItems;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Eight Facades of one block around an empty centre give eight Hollow Facades of that block. */
public class HollowFacadeRecipe extends CustomRecipe {

    public static final int RESULT_COUNT = 8;
    private static final int CENTRE = 4;

    /** The recipe has no settings, so one instance serves every recipe file. */
    public static final HollowFacadeRecipe INSTANCE = new HollowFacadeRecipe();

    private HollowFacadeRecipe() {
    }

    /** The ring's block, or null when the grid is not a ring of solid Facades of one block. */
    private static Block ringBlock(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3 || !input.getItem(CENTRE).isEmpty()) {
            return null;
        }
        Block block = null;
        for (int i = 0; i < input.size(); i++) {
            if (i == CENTRE) {
                continue;
            }
            ItemStack stack = input.getItem(i);
            if (!stack.is(ServusLogisticsItems.FACADE.get())) {
                return null;
            }
            Block slotBlock = FacadeItem.blockOf(stack);
            if (block != null && block != slotBlock) {
                return null;
            }
            block = slotBlock;
        }
        return block;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return ringBlock(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Block block = ringBlock(input);
        return block == null ? ItemStack.EMPTY : FacadeItem.of(block, RESULT_COUNT, true);
    }


    @Override
    public RecipeSerializer<HollowFacadeRecipe> getSerializer() {
        return ServusLogisticsRecipes.HOLLOW_FACADE.get();
    }
}
