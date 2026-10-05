package io.github.dirnei.mc.servus.lib.network.core;

/**
 * Creates a network object for a new graph component.
 *
 * @param <N> node type
 * @param <E> event type
 * @param <T> network type
 */
@FunctionalInterface
public interface NetworkFactory<N, E, T extends ServusNetwork<N, E>> {

    T create(int id, NetworkSet<N, E, T> owner);
}
