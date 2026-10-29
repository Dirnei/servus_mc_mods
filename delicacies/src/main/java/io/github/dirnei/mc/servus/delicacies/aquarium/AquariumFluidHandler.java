package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.Optional;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The aquarium's fluid side, the same from every part: water goes into the tank, and what comes out is the tank's
 * water as Murky Water, taking the same share of the dirt with it. Other fluids are refused.
 */
final class AquariumFluidHandler extends LegacyFluidHandler {

    private final AquariumPartBlockEntity master;

    AquariumFluidHandler(AquariumPartBlockEntity master) {
        this.master = master;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        long water = master.water();
        return water <= 0 ? FluidStack.EMPTY : new FluidStack(DelicaciesFluids.MURKY_WATER.get(), (int) Math.min(Integer.MAX_VALUE, water));
    }

    @Override
    public int getTankCapacity(int tank) {
        return master.tankCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return isWater(stack) || isMurky(stack);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (!isWater(resource)) {
            return 0;
        }
        Optional<TankNetwork> network = master.network();
        if (network.isEmpty()) {
            return 0;
        }
        int filled = network.get().fill(Conversions.toNodePos(master.tankPos()), resource, simulate);
        if (filled > 0 && !simulate) {
            master.setChanged();
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        return isMurky(resource) ? drain(resource.getAmount(), simulate) : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        Optional<TankNetwork> network = master.network();
        long water = master.water();
        if (network.isEmpty() || water <= 0 || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = network.get().drain(Conversions.toNodePos(master.tankPos()), new FluidStack(Fluids.WATER, maxDrain), simulate);
        if (drained.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (!simulate) {
            master.state().takeDirt(AquariumWater.dirtTaken(master.state().dirt(), drained.getAmount(), water));
        }
        return new FluidStack(DelicaciesFluids.MURKY_WATER.get(), drained.getAmount());
    }

    private static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().isSame(Fluids.WATER);
    }

    private static boolean isMurky(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().isSame(DelicaciesFluids.MURKY_WATER.get());
    }
}
