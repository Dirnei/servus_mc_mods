package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyEnergyHandler;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyItemHandler;
import io.github.dirnei.mc.servus.foundation.transfer.LegacyFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The capability handlers of a Sink. They accept within the Sink's budget,
 * destroy what they accept and never hold or give anything.
 */
final class SinkHandlers {

    private SinkHandlers() {
    }

    /** One empty slot that takes any item. */
    static final class Items extends LegacyItemHandler {

        private final SinkBlockEntity sink;

        Items(SinkBlockEntity sink) {
            this.sink = sink;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return stack;
            }
            long granted = sink.grantItems(stack.getCount(), simulate);
            if (granted <= 0) {
                return stack;
            }
            return granted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - (int) granted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    /** One endless empty tank that takes any fluid; an unlimited one ignores the budget. */
    static final class Fluid extends LegacyFluidHandler {

        private final SinkBlockEntity sink;
        private final boolean unlimited;

        Fluid(SinkBlockEntity sink, boolean unlimited) {
            this.sink = sink;
            this.unlimited = unlimited;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, boolean simulate) {
            if (resource.isEmpty()) {
                return 0;
            }
            if (unlimited) {
                if (!simulate) {
                    sink.markVoided();
                }
                return resource.getAmount();
            }
            return (int) sink.grantFluid(resource.getAmount(), simulate);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean simulate) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, boolean simulate) {
            return FluidStack.EMPTY;
        }
    }

    /** An energy storage that is always empty and takes FE within the budget. */
    static final class Energy extends LegacyEnergyHandler {

        private final SinkBlockEntity sink;

        Energy(SinkBlockEntity sink) {
            this.sink = sink;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return toReceive <= 0 ? 0 : (int) sink.grantEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }
    }
}
