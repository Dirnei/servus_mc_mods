package io.github.dirnei.mc.servus.manufacturing.alchemy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * One tank per principle, each holding only its own fluid. In give mode the handler only drains
 * (the Calciner); in accept mode it only fills (the Coagulator). The machine itself moves fluid
 * through {@link #insert} and {@link #take}, whatever the mode.
 */
public final class PrincipleTanks implements IFluidHandler {

    private final FluidTank[] tanks = new FluidTank[Principle.values().length];
    private final IFluidHandler[] views = new IFluidHandler[Principle.values().length];
    private final boolean gives;
    private final Runnable changed;

    public PrincipleTanks(int capacity, boolean gives, Runnable changed) {
        this.gives = gives;
        this.changed = changed;
        for (Principle principle : Principle.values()) {
            tanks[principle.ordinal()] = new FluidTank(capacity, stack -> stack.getFluid().isSame(PrincipleFluids.fluid(principle))) {
                @Override
                protected void onContentsChanged() {
                    changed.run();
                }
            };
            views[principle.ordinal()] = new View(principle);
        }
    }

    /** A handler of one principle's tank only, in the same direction as the whole: what a side set to that tank exposes. */
    public IFluidHandler view(Principle principle) {
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
        for (FluidTank tank : tanks) {
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
                tanks[principle.ordinal()].fill(new FluidStack(PrincipleFluids.fluid(principle), amount), FluidAction.EXECUTE);
            }
        }
    }

    /** Removes the amounts from their tanks; the caller checks that they are there. */
    public void take(Principles amounts) {
        for (Principle principle : Principle.values()) {
            int amount = amounts.get(principle);
            if (amount > 0) {
                tanks[principle.ordinal()].drain(amount, FluidAction.EXECUTE);
            }
        }
    }

    /** The tank of one principle, for pushing it out. */
    public FluidTank tank(Principle principle) {
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
    public int fill(FluidStack resource, FluidAction action) {
        if (gives || resource.isEmpty()) {
            return 0;
        }
        Principle principle = PrincipleFluids.principleOf(resource.getFluid());
        return principle == null ? 0 : tanks[principle.ordinal()].fill(resource, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (!gives || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Principle principle = PrincipleFluids.principleOf(resource.getFluid());
        return principle == null ? FluidStack.EMPTY : tanks[principle.ordinal()].drain(resource, action);
    }

    /** Drains from the first tank that holds anything. */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (!gives) {
            return FluidStack.EMPTY;
        }
        for (FluidTank tank : tanks) {
            if (!tank.isEmpty()) {
                return tank.drain(maxDrain, action);
            }
        }
        return FluidStack.EMPTY;
    }

    private final class View implements IFluidHandler {

        private final Principle principle;

        private View(Principle principle) {
            this.principle = principle;
        }

        private FluidTank tank() {
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
        public int fill(FluidStack resource, FluidAction action) {
            return gives || resource.isEmpty() ? 0 : tank().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return !gives || resource.isEmpty() ? FluidStack.EMPTY : tank().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return gives ? tank().drain(maxDrain, action) : FluidStack.EMPTY;
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
            int amount = tag.getInt(principle.id());
            Fluid fluid = PrincipleFluids.fluid(principle);
            tanks[principle.ordinal()].setFluid(amount > 0 ? new FluidStack(fluid, amount) : FluidStack.EMPTY);
        }
    }
}
