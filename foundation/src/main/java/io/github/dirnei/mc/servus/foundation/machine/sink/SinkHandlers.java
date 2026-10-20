package io.github.dirnei.mc.servus.foundation.machine.sink;

import io.github.dirnei.mc.servus.foundation.transfer.LegacyEnergyHandler;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * The capability handlers of a Sink: an insert-only view of the buffer for
 * items and fluid, and an energy storage that destroys FE within the budget.
 */
final class SinkHandlers {

    private SinkHandlers() {
    }

    /** The buffer as automation sees it: everything can go in, nothing comes out. */
    static final class InsertOnly<T extends Resource> extends DelegatingResourceHandler<T> {

        InsertOnly(ResourceHandler<T> buffer) {
            super(buffer);
        }

        @Override
        public int extract(int index, T resource, int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(T resource, int amount, TransactionContext transaction) {
            return 0;
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
