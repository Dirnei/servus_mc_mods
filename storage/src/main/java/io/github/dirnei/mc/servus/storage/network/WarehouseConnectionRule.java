package io.github.dirnei.mc.servus.storage.network;

import io.github.dirnei.mc.servus.foundation.graph.Side;

/**
 * Which warehouse nodes link up when they touch. Stock holding blocks,
 * inbound stations and garages link to each other and to rails on any side. Rails link
 * sideways to rails and to elevator segments. Segments link vertically to
 * segments and sideways to rails, never straight to a storage block or a
 * station. Pure, no Minecraft imports.
 */
public final class WarehouseConnectionRule {

    /** What a node is, as far as linking is concerned. */
    public enum NodeKind { STOCK, RAIL, ELEVATOR, STATION, GARAGE, RACK }

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
            case STOCK, STATION, GARAGE -> to != NodeKind.ELEVATOR && to != NodeKind.RACK;
            // A rack links only to rails (its front, checked by the network) and to bays of its own section.
            case RACK -> to == NodeKind.RAIL || to == NodeKind.RACK;
            case RAIL -> to == NodeKind.STOCK || to == NodeKind.STATION || to == NodeKind.GARAGE || to == NodeKind.RACK || !vertical;
            case ELEVATOR -> switch (to) {
                case STOCK, STATION, GARAGE, RACK -> false;
                case RAIL -> !vertical;
                case ELEVATOR -> vertical;
            };
        };
    }
}
