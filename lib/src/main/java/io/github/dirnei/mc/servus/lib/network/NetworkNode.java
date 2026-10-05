package io.github.dirnei.mc.servus.lib.network;

/**
 * Implemented by block entities that take part in a network. The block entity
 * tells the manager which type it belongs to and which value represents it in
 * the graph; the value is often the block entity itself.
 *
 * @param <N> node type stored in the graph
 */
public interface NetworkNode<N> {

    NetworkType<N, ?, ?> networkType();

    N networkNode();
}
