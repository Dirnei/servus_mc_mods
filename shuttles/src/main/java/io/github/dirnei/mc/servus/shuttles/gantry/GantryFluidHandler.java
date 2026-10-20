package io.github.dirnei.mc.servus.shuttles.gantry;

import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import java.util.List;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Both tanks of a gantry behind one handler. Fills follow the locks in the
 * order of {@link GantryTanks#fillOrder}; drains take from the tank holding
 * the most of the fluid first, then from the other. A handler made for
 * filling only drains nothing and one for draining only fills nothing.
 */
final class GantryFluidHandler extends LegacyFluidHandler {

    private final FluidGantryBlockEntity gantry;
    private final boolean fills;
    private final boolean drains;

    GantryFluidHandler(FluidGantryBlockEntity gantry, boolean fills, boolean drains) {
        this.gantry = gantry;
        this.fills = fills;
        this.drains = drains;
    }

    @Override
    public int getTanks() {
        return 2;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return gantry.tank(tank).getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return gantry.tank(tank).getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return fills && gantry.rule(tank).accepts(stack.getFluid());
    }

    /** The handler picks the tank itself, so a move through either tank index fills or drains the gantry. */
    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        return extract(resource, amount, transaction);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (!fills || resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (int tank : GantryTanks.fillOrder(gantry.rules(), resource.getFluid())) {
            filled += gantry.tank(tank).fill(resource.copyWithAmount(resource.getAmount() - filled), simulate);
            if (filled >= resource.getAmount()) {
                break;
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (!drains || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = 0;
        for (FluidSlot tank : fullestFirst(resource)) {
            drained += tank.drain(resource.copyWithAmount(resource.getAmount() - drained), simulate).getAmount();
            if (drained >= resource.getAmount()) {
                break;
            }
        }
        return drained <= 0 ? FluidStack.EMPTY : resource.copyWithAmount(drained);
    }

    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        FluidSlot left = gantry.tank(0);
        FluidSlot right = gantry.tank(1);
        FluidSlot fuller = left.getFluidAmount() >= right.getFluidAmount() ? left : right;
        return fuller.isEmpty() ? FluidStack.EMPTY : drain(fuller.getFluid().copyWithAmount(maxDrain), simulate);
    }

    /** The tanks holding the fluid, the one holding more first. */
    private List<FluidSlot> fullestFirst(FluidStack resource) {
        FluidSlot left = gantry.tank(0);
        FluidSlot right = gantry.tank(1);
        boolean leftHolds = FluidStack.isSameFluidSameComponents(left.getFluid(), resource);
        boolean rightHolds = FluidStack.isSameFluidSameComponents(right.getFluid(), resource);
        if (leftHolds && rightHolds) {
            return left.getFluidAmount() >= right.getFluidAmount() ? List.of(left, right) : List.of(right, left);
        }
        return leftHolds ? List.of(left) : rightHolds ? List.of(right) : List.of();
    }
}
