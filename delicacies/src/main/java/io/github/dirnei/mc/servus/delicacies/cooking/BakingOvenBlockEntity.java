package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/** The Baking Oven: a vessel without a tank that bakes baking recipes. */
public class BakingOvenBlockEntity extends VesselBlockEntity<BakingRecipe> {

    public BakingOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.BAKING_OVEN.get(), pos, state);
    }

    @Override
    protected RecipeType<BakingRecipe> recipeType() {
        return ServusDelicaciesRecipes.BAKING.get();
    }
}
