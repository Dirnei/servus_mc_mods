package io.github.dirnei.mc.servus.foundation.logistics.tank;

import io.github.dirnei.mc.servus.foundation.logistics.facade.FacadeRecipe;
import io.github.dirnei.mc.servus.foundation.logistics.registry.ServusLogisticsRecipes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The Fluid Tank recipe: copper ingots in the corners, glass on the top, left and right
 * edges, an empty centre, and in the bottom middle any solid full block, which becomes
 * the tank's floor.
 */
public class FluidTankRecipe extends CustomRecipe {

    private static final int SIZE = 3;
    private static final int FLOOR_SLOT = 7;
    private static final int CENTRE_SLOT = 4;

    /** The recipe has no settings, so one instance serves every recipe file. */
    public static final FluidTankRecipe INSTANCE = new FluidTankRecipe();

    private FluidTankRecipe() {
    }

    /** The floor block the grid asks for, or null when the grid is not a tank recipe. */
    public static Block floorOf(CraftingInput input) {
        if (input.width() != SIZE || input.height() != SIZE) {
            return null;
        }
        Block floor = null;
        for (int i = 0; i < SIZE * SIZE; i++) {
            ItemStack stack = input.getItem(i);
            boolean corner = i == 0 || i == 2 || i == 6 || i == 8;
            if (corner) {
                if (!stack.is(Items.COPPER_INGOT)) {
                    return null;
                }
            } else if (i == CENTRE_SLOT) {
                if (!stack.isEmpty()) {
                    return null;
                }
            } else if (i == FLOOR_SLOT) {
                if (!(stack.getItem() instanceof BlockItem blockItem) || !FacadeRecipe.isFacadeable(blockItem.getBlock())) {
                    return null;
                }
                floor = blockItem.getBlock();
            } else if (!stack.is(Items.GLASS)) {
                return null;
            }
        }
        return floor;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return floorOf(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        Block floor = floorOf(input);
        return floor == null ? ItemStack.EMPTY : FluidTankItem.withFloor(floor);
    }

    @Override
    public RecipeSerializer<FluidTankRecipe> getSerializer() {
        return ServusLogisticsRecipes.FLUID_TANK.get();
    }
}
