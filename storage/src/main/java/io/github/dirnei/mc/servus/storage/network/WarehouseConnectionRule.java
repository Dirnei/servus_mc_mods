package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.lib.graph.Side;

/**
 * Which warehouse nodes link up when they touch. Stock holding blocks link to
 * each other and to rails on any side. Rails link sideways to rails and to
 * elevator segments. Segments link vertically to segments and sideways to
 * rails, never straight to a storage block. Pure, no Minecraft imports.
 */
public final class WarehouseConnectionRule {

    /** What a node is, as far as linking is concerned. */
    public enum NodeKind { STOCK, RAIL, ELEVATOR }

    private WarehouseConnectionRule() {
    }

    /**
     * @param from the node being asked
     * @param side the side of {@code from} that faces {@code to}
     * @param to   the neighbour
     */
    public static boolean accepts(NodeKind from, Side side, NodeKind to) {
        boolean vertical = side == Side.UP || side == Side.DOWN;
        return switch (from) {
            case STOCK -> to != NodeKind.ELEVATOR;
            case RAIL -> to == NodeKind.STOCK || !vertical;
            case ELEVATOR -> switch (to) {
                case STOCK -> false;
                case RAIL -> !vertical;
                case ELEVATOR -> vertical;
            };
        };
    }
}
