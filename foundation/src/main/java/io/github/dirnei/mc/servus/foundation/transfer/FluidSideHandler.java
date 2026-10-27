package io.github.dirnei.mc.servus.foundation.transfer;

import io.github.dirnei.mc.servus.foundation.side.FluidSideRules;
import io.github.dirnei.mc.servus.foundation.side.FluidSides;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/**
 * The fluid capability of one world face of a block with a Fluids layer: it shows the tanks
 * the side's tank choice reaches, and fills and drains them as the side's mode and the tanks'
 * roles allow. A side on Off moves nothing but keeps pipes connected.
 */
public final class FluidSideHandler extends LegacyFluidHandler {

    private final FluidSideBlock block;
    private final Direction face;

    private FluidSideHandler(FluidSideBlock block, Direction face) {
        this.block = block;
        this.face = face;
    }

    /** The handler for a world face, or null when the block has no Fluids layer or fixes that side. */
    public static @Nullable FluidSideHandler of(FluidSideBlock block, @Nullable Direction face) {
        FluidSides sides = block.fluidSides();
        if (sides == null || face == null || sides.isLocked(block.relativeSide(face))) {
            return null;
        }
        return new FluidSideHandler(block, face);
    }

    private RelativeSide side() {
        return block.relativeSide(face);
    }

    private List<Integer> reached() {
        FluidSides sides = block.fluidSides();
        List<Integer> tanks = new ArrayList<>();
        for (int tank = 0; tank < block.sideSlots().size(); tank++) {
            if (sides.reaches(side(), tank)) {
                tanks.add(tank);
            }
        }
        return tanks;
    }

    private FluidSlot slot(int tank) {
        return block.sideSlots().get(tank);
    }

    @Override
    public int getTanks() {
        return reached().size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        List<Integer> reached = reached();
        return tank >= 0 && tank < reached.size() ? slot(reached.get(tank)).getFluid().copy() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        List<Integer> reached = reached();
        return tank >= 0 && tank < reached.size() ? slot(reached.get(tank)).getCapacity() : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        List<Integer> reached = reached();
        if (tank < 0 || tank >= reached.size() || !FluidSideRules.allowsFill(block.fluidSides().mode(side()))) {
            return false;
        }
        int index = reached.get(tank);
        return block.sideTanks().get(index).role().fills() && slot(index).isFluidValid(stack)
                && block.sideTankAccepts(index, FluidResource.of(stack));
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return 0;
        }
        List<Integer> order = FluidSideRules.fillOrder(block.fluidSides(), side(), block.sideTankStates(), FluidResource.of(resource),
                block::sideTankAccepts);
        int filled = 0;
        for (int tank : order) {
            if (filled >= resource.getAmount()) {
                break;
            }
            filled += slot(tank).fill(resource.copyWithAmount(resource.getAmount() - filled), simulate);
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        FluidResource wanted = FluidResource.of(resource);
        int drained = 0;
        for (int tank : FluidSideRules.drainOrder(block.fluidSides(), side(), block.sideTankStates())) {
            if (drained >= resource.getAmount()) {
                break;
            }
            if (wanted.matches(slot(tank).getFluid())) {
                drained += slot(tank).drain(resource.copyWithAmount(resource.getAmount() - drained), simulate).getAmount();
            }
        }
        return drained <= 0 ? FluidStack.EMPTY : wanted.toStack(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        List<Integer> order = FluidSideRules.drainOrder(block.fluidSides(), side(), block.sideTankStates());
        if (order.isEmpty() || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        return drain(slot(order.getFirst()).getFluid().copyWithAmount(maxDrain), simulate);
    }
}
