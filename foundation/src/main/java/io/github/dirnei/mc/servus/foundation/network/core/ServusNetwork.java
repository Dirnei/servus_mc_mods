package io.github.dirnei.mc.servus.foundation.network.core;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.Set;

/**
 * One network: the object behind one connected component of a
 * {@link NetworkSet}. Subclasses keep whatever state the network needs, handle
 * events from the mailbox and do their per tick work in {@link #onTick()}.
 *
 * <p>All hooks run on the server thread. Only {@link #post(Object)} may be
 * called from other threads.
 *
 * @param <N> node type stored in the graph
 * @param <E> event type delivered through the mailbox
 */
public abstract class ServusNetwork<N, E> {

    private final int id;
    private final NetworkSet<N, E, ?> owner;
    private final Mailbox<E> mailbox = new Mailbox<>();
    private boolean disposed;

    protected ServusNetwork(int id, NetworkSet<N, E, ?> owner) {
        this.id = id;
        this.owner = owner;
    }

    public final int id() {
        return id;
    }

    public final NetworkSet<N, E, ?> owner() {
        return owner;
    }

    public final boolean isDisposed() {
        return disposed;
    }

    /** Positions of all nodes in this network. Read only view, valid until the next change. */
    public final Set<NodePos> members() {
        return owner.graph().members(id);
    }

    public final int size() {
        return members().size();
    }

    /** Queues an event for the next tick. Safe to call from any thread. */
    public final void post(E event) {
        mailbox.post(event);
    }

    final Mailbox<E> mailbox() {
        return mailbox;
    }

    final void markDisposed() {
        disposed = true;
        mailbox.clear();
        onDisposed();
    }

    /** Handles one event from the mailbox. Called in posting order before {@link #onTick()}. */
    protected abstract void handle(E event);

    /** Per tick work, after the mailbox was drained. */
    protected void onTick() {
    }

    /** A node joined this network. */
    protected void onNodeAdded(NodePos pos, N node) {
    }

    /** A node is about to leave this network. It is still a member while this runs. */
    protected void onNodeRemoved(NodePos pos, N node) {
    }

    /**
     * Another network of the same type is being merged into this one. Its
     * members are already part of this network. Take over whatever state
     * matters before {@code other} is disposed right after this call.
     */
    protected void onAbsorb(ServusNetwork<N, E> other) {
    }

    /** This network no longer exists. Release resources. */
    protected void onDisposed() {
    }
}
