package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/** The Pan: a vessel without a tank. */
public class PanBlockEntity extends VesselBlockEntity<PanRecipe> {

    public PanBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.PAN.get(), pos, state);
    }

    @Override
    protected RecipeType<PanRecipe> recipeType() {
        return ServusDelicaciesRecipes.PAN.get();
    }
}
