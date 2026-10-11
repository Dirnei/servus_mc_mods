package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import io.github.dirnei.mc.servus.foundation.transfer.FluidView;
import io.github.dirnei.mc.servus.foundation.transfer.FluidSlot;

/**
 * One tank per principle, each holding only its own fluid. In give mode the handler only drains
 * (the Calciner); in accept mode it only fills (the Coagulator). The machine itself moves fluid
 * through {@link #insert} and {@link #take}, whatever the mode.
 */
public final class PrincipleTanks extends LegacyFluidHandler {

    private final FluidSlot[] tanks = new FluidSlot[Principle.values().length];
    private final FluidView[] views = new FluidView[Principle.values().length];
    private final boolean gives;
    private final Runnable changed;

    public PrincipleTanks(int capacity, boolean gives, Runnable changed) {
        this.gives = gives;
        this.changed = changed;
        for (Principle principle : Principle.values()) {
            tanks[principle.ordinal()] = new FluidSlot(capacity, stack -> stack.getFluid().isSame(PrincipleFluids.fluid(principle))) {
                @Override
                protected void onContentsChanged() {
                    changed.run();
                }
            };
            views[principle.ordinal()] = new View(principle);
        }
    }

    /** A handler of one principle's tank only, in the same direction as the whole: what a side set to that tank exposes. */
    public FluidView view(Principle principle) {
        return views[principle.ordinal()];
    }

    /** The tanks a machine with these tanks offers in its Fluids layer, in principle order. */
    public static java.util.List<io.github.dirnei.mc.servus.foundation.side.SideTank> sideTanks(boolean input) {
        java.util.List<io.github.dirnei.mc.servus.foundation.side.SideTank> list = new java.util.ArrayList<>();
        for (Principle principle : Principle.values()) {
            list.add(new io.github.dirnei.mc.servus.foundation.side.SideTank(
                    "fluid_type." + io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds.MOD_ID + "." + principle.id(),
                    principle.color(), input));
        }
        return java.util.List.copyOf(list);
    }

    public void setCapacity(int capacity) {
        for (FluidSlot tank : tanks) {
            tank.setCapacity(capacity);
        }
    }

    public int capacity() {
        return tanks[0].getCapacity();
    }

    /** What the three tanks hold now. */
    public Principles contents() {
        return new Principles(amount(Principle.SALT), amount(Principle.SULFUR), amount(Principle.MERCURY));
    }

    public int amount(Principle principle) {
        return tanks[principle.ordinal()].getFluidAmount();
    }

    /** Adds the amounts to their tanks; the caller checks that they fit. */
    public void insert(Principles amounts) {
        for (Principle principle : Principle.values()) {
            int amount = amounts.get(principle);
            if (amount > 0) {
                tanks[principle.ordinal()].fill(new FluidStack(PrincipleFluids.fluid(principle), amount), false);
            }
        }
    }

    /** Removes the amounts from their tanks; the caller checks that they are there. */
    public void take(Principles amounts) {
        for (Principle principle : Principle.values()) {
            int amount = amounts.get(principle);
            if (amount > 0) {
                tanks[principle.ordinal()].drain(amount, false);
            }
        }
    }

    /** The tank of one principle, for pushing it out. */
    public FluidSlot tank(Principle principle) {
        return tanks[principle.ordinal()];
    }

    @Override
    public int getTanks() {
        return tanks.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tanks[tank].getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return tanks[tank].getCapacity();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tanks[tank].isFluidValid(stack);
    }

    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (gives || resource.isEmpty()) {
            return 0;
        }
        Principle principle = PrincipleFluids.principleOf(resource.getFluid());
        return principle == null ? 0 : tanks[principle.ordinal()].fill(resource, simulate);
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean simulate) {
        if (!gives || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Principle principle = PrincipleFluids.principleOf(resource.getFluid());
        return principle == null ? FluidStack.EMPTY : tanks[principle.ordinal()].drain(resource, simulate);
    }

    /** Drains from the first tank that holds anything. */
    @Override
    public FluidStack drain(int maxDrain, boolean simulate) {
        if (!gives) {
            return FluidStack.EMPTY;
        }
        for (FluidSlot tank : tanks) {
            if (!tank.isEmpty()) {
                return tank.drain(maxDrain, simulate);
            }
        }
        return FluidStack.EMPTY;
    }

    private final class View extends LegacyFluidHandler {

        private final Principle principle;

        private View(Principle principle) {
            this.principle = principle;
        }

        private FluidSlot tank() {
            return tanks[principle.ordinal()];
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank().getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank().getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank().isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            return gives || resource.isEmpty() ? 0 : tank().fill(resource, simulate);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return !gives || resource.isEmpty() ? FluidStack.EMPTY : tank().drain(resource, simulate);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            return gives ? tank().drain(maxDrain, simulate) : FluidStack.EMPTY;
        }
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        for (Principle principle : Principle.values()) {
            tag.putInt(principle.id(), amount(principle));
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Principle principle : Principle.values()) {
            int amount = tag.getIntOr(principle.id(), 0);
            Fluid fluid = PrincipleFluids.fluid(principle);
            tanks[principle.ordinal()].setFluid(amount > 0 ? new FluidStack(fluid, amount) : FluidStack.EMPTY);
        }
    }
}
