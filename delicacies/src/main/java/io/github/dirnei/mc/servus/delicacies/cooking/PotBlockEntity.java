package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenStatsManager;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

/** The Pot: a vessel with a tank that takes only the fluids pot recipes use. */
public class PotBlockEntity extends TankVesselBlockEntity<PotRecipe> {

    public PotBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.POT.get(), pos, state, KitchenStatsManager.INSTANCE.stats().potCapacity());
    }

    @Override
    protected RecipeType<PotRecipe> recipeType() {
        return ServusDelicaciesRecipes.POT.get();
    }
}
