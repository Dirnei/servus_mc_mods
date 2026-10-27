package io.github.dirnei.mc.servus.foundation.transfer;

import io.github.dirnei.mc.servus.foundation.block.SidedBlockEntity;
import io.github.dirnei.mc.servus.foundation.side.FluidSideRules.TankState;
import io.github.dirnei.mc.servus.foundation.side.SideTank;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * A block entity whose Fluids layer reaches real tanks: what {@link FluidSideHandler} and
 * {@link FluidSideMover} need besides the side configuration.
 */
public interface FluidSideBlock extends SidedBlockEntity {

    /** The tanks in the same index order as {@link #sideTanks()}. */
    List<FluidSlot> sideSlots();

    /** Whether a tank takes this fluid at all, for example only its kept fluid; true by default. */
    default boolean sideTankAccepts(int tank, FluidResource fluid) {
        return true;
    }

    /** A snapshot of the tanks for {@link io.github.dirnei.mc.servus.foundation.side.FluidSideRules}. */
    default List<TankState<FluidResource>> sideTankStates() {
        List<SideTank> tanks = sideTanks();
        List<FluidSlot> slots = sideSlots();
        List<TankState<FluidResource>> states = new ArrayList<>(slots.size());
        for (int tank = 0; tank < slots.size(); tank++) {
            FluidSlot slot = slots.get(tank);
            FluidResource key = slot.isEmpty() ? null : FluidResource.of(slot.getFluid());
            states.add(new TankState<>(key, slot.getFluidAmount(), slot.getCapacity(), tanks.get(tank).role()));
        }
        return states;
    }
}
