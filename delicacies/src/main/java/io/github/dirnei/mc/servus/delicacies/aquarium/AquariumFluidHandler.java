package io.github.dirnei.mc.servus.delicacies.aquarium;

import io.github.dirnei.mc.servus.delicacies.registry.DelicaciesFluids;
import io.github.dirnei.mc.servus.foundation.logistics.tank.TankNetwork;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.Optional;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The aquarium's fluid side, the same from every part: water goes into the tank, and what comes out is the Murky
 * Water the filter made; the tank's water never leaves through a part. Other fluids are refused.
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
        int murky = master.state().murky();
        return murky <= 0 ? FluidStack.EMPTY : new FluidStack(DelicaciesFluids.MURKY_WATER.get(), murky);
    }

    @Override
    public int getTankCapacity(int tank) {
        return AquariumPartBlockEntity.stats().murkyCapacity();
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
        int drained = master.state().takeMurky(maxDrain, simulate);
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(DelicaciesFluids.MURKY_WATER.get(), drained);
    }

    private static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().isSame(Fluids.WATER);
    }

    private static boolean isMurky(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().isSame(DelicaciesFluids.MURKY_WATER.get());
    }
}
