package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.delicacies.kitchen.KitchenStatsManager;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesBlockEntities;
import io.github.dirnei.mc.servus.delicacies.registry.ServusDelicaciesRecipes;
import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** The Pot: a vessel with a tank that takes only the fluids pot recipes use. */
public class PotBlockEntity extends VesselBlockEntity<PotRecipe> {

    private final FluidSlot tank = new FluidSlot(KitchenStatsManager.INSTANCE.stats().potCapacity(), this::takesFluid) {
        @Override
        protected void onContentsChanged() {
            contentsChanged();
        }
    };

    public PotBlockEntity(BlockPos pos, BlockState state) {
        super(ServusDelicaciesBlockEntities.POT.get(), pos, state);
    }

    @Override
    protected RecipeType<PotRecipe> recipeType() {
        return ServusDelicaciesRecipes.POT.get();
    }

    private boolean takesFluid(FluidStack fluid) {
        return level == null || ServusRecipes.all(level, recipeType()).stream().anyMatch(holder -> holder.value().acceptsFluid(fluid));
    }

    @Override
    public FluidStack fluid() {
        return tank.getFluid();
    }

    public int capacity() {
        return tank.getCapacity();
    }

    @Override
    protected void drainFluid(int amount) {
        tank.drain(amount, false);
    }

    /** The tank as automation sees it on every side. */
    public ResourceHandler<FluidResource> fluidCapability() {
        return tank;
    }

    @Override
    public boolean useFluidContainer(Player player, InteractionHand hand) {
        if (!Transfer.isFluidContainer(player.getItemInHand(hand))) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            FluidUtil.interactWithFluidHandler(player, hand, worldPosition, tank, null);
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "Fluid");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "Fluid");
    }
}
