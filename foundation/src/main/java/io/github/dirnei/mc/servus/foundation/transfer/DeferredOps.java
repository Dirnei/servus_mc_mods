package io.github.dirnei.mc.servus.foundation.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Operations promised inside a transaction and carried out once the root
 * transaction commits. An aborted (nested) transaction drops the operations
 * it added. This lets handlers whose work cannot be undone (routing into a
 * duct network, voiding in the Sink, a tank network spread over many block
 * entities) take part in transactions: inside the transaction they only
 * simulate, on the root commit they execute.
 *
 * <p>Root commit callbacks may open new root transactions, so the executed
 * operations can move items through other handlers.
 *
 * @param <O> the operation record
 */
final class DeferredOps<O> extends SnapshotJournal<Integer> {

    private final List<O> pending = new ArrayList<>();
    private final Consumer<O> executor;

    DeferredOps(Consumer<O> executor) {
        this.executor = executor;
    }

    /** Records an operation in the transaction. */
    void add(O op, TransactionContext transaction) {
        updateSnapshots(transaction);
        pending.add(op);
    }

    /** Sum of {@code amount} over the pending operations that match. */
    int sum(Predicate<O> match, java.util.function.ToIntFunction<O> amount) {
        int total = 0;
        for (O op : pending) {
            if (match.test(op)) {
                total += amount.applyAsInt(op);
            }
        }
        return total;
    }

    /** {@code a + b}, capped at {@code Integer.MAX_VALUE}. */
    static int add(int a, int b) {
        return (int) Math.min(Integer.MAX_VALUE, (long) a + b);
    }

    @Override
    protected Integer createSnapshot() {
        return pending.size();
    }

    @Override
    protected void revertToSnapshot(Integer snapshot) {
        while (pending.size() > snapshot) {
            pending.remove(pending.size() - 1);
        }
    }

    @Override
    protected void onRootCommit(Integer originalState) {
        List<O> run = new ArrayList<>(pending);
        pending.clear();
        for (O op : run) {
            executor.accept(op);
        }
    }
}
