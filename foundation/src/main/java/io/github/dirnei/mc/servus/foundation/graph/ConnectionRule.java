package io.github.dirnei.mc.servus.foundation.graph;

/**
 * Decides whether a node connects to its neighbour on a given side. The graph
 * asks both nodes, each from its own point of view, and only creates an edge
 * when both accept.
 *
 * @param <N> the node type stored in the graph
 */
@FunctionalInterface
public interface ConnectionRule<N> {

    /**
     * @param from     position of the node being asked
     * @param fromNode the node being asked
     * @param side     side of {@code from} that faces {@code to}
     * @param to       position of the neighbour
     * @param toNode   the neighbour
     * @return true when {@code fromNode} wants to connect to {@code toNode} through {@code side}
     */
    boolean canConnect(NodePos from, N fromNode, Side side, NodePos to, N toNode);

    static <N> ConnectionRule<N> always() {
        return (from, fromNode, side, to, toNode) -> true;
    }
}
