package io.github.dirnei.mc.servus.delicacies.cooking;

import io.github.dirnei.mc.servus.foundation.production.ServusRecipes;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * A vessel with one tank that takes only the fluids its recipes use, by hand with a fluid container and by
 * automation on every side.
 *
 * @param <R> the vessel's recipe kind
 */
public abstract class TankVesselBlockEntity<R extends VesselRecipe> extends VesselBlockEntity<R> {

    private final FluidSlot tank;

    protected TankVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.tank = new FluidSlot(capacity, this::tankTakes) {
            @Override
            protected void onContentsChanged() {
                contentsChanged();
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return tankGives() ? super.extract(index, resource, amount, transaction) : 0;
            }
        };
    }

    /** Whether the tank takes {@code fluid} now: by default when some recipe of this vessel uses it. */
    protected boolean tankTakes(FluidStack fluid) {
        return level == null || ServusRecipes.all(level, recipeType()).stream().anyMatch(holder -> holder.value().acceptsFluid(fluid));
    }

    /** Whether fluid may leave the tank now, by hand, by automation or for a recipe; by default always. */
    protected boolean tankGives() {
        return true;
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
