package io.github.dirnei.mc.servus.foundation.transfer;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Base of energy handlers written with {@code receiveEnergy} and
 * {@code extractEnergy} and a simulate flag, exposed through the transfer API. Inside a transaction the handler
 * only simulates; the real transfer runs when the root transaction commits
 * (see {@link DeferredOps}).
 */
public abstract class LegacyEnergyHandler implements EnergyHandler {

    private record Op(boolean receive, int amount) {
    }

    private final DeferredOps<Op> ops = new DeferredOps<>(op -> {
        if (op.receive()) {
            receiveEnergy(op.amount(), false);
        } else {
            extractEnergy(op.amount(), false);
        }
    });

    public abstract int receiveEnergy(int toReceive, boolean simulate);

    public abstract int extractEnergy(int toExtract, boolean simulate);

    public abstract int getEnergyStored();

    public abstract int getMaxEnergyStored();

    @Override
    public long getAmountAsLong() {
        return getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        int pending = ops.sum(Op::receive, Op::amount);
        int accepted = Math.max(0, Math.min(amount, receiveEnergy(DeferredOps.cappedSum(pending, amount), true) - pending));
        if (accepted > 0) {
            ops.add(new Op(true, accepted), transaction);
        }
        return accepted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        int pending = ops.sum(op -> !op.receive(), Op::amount);
        int taken = Math.max(0, Math.min(amount, extractEnergy(DeferredOps.cappedSum(pending, amount), true) - pending));
        if (taken > 0) {
            ops.add(new Op(false, taken), transaction);
        }
        return taken;
    }
}
