package io.github.dirnei.mc.servus.shuttles.network;

import io.github.dirnei.mc.servus.foundation.graph.Side;

/**
 * Which warehouse nodes link up when they touch. {@code STOCK} is the
 * Warehouse Controller: it links to rails and to another controller on any
 * side, so two touching controllers make one offline network, and to
 * elevator segments beside it. Blocks shuttles
 * serve (racks, stations, Access Points, garages, Fluid
 * Racks, fluid stations) are rail only: they link only to rails (racks and Fluid Racks also
 * to bays of their own section), so
 * they never join or pass on a warehouse by touching it. Rails link sideways to rails and to
 * elevator segments. Segments link vertically to segments and sideways to
 * rails and the controller, never to a rack or a station.
 */
public final class WarehouseConnectionRule {

    /** What a node is, as far as linking is concerned; {@code STOCK} is the controller. */
    public enum NodeKind { STOCK, RAIL, ELEVATOR, STATION, GARAGE, RACK, FLUID_RACK, FLUID_STATION }

    private WarehouseConnectionRule() {
    }

    /** Whether shuttles serve the kind, so it joins a warehouse only through rails. */
    public static boolean railOnly(NodeKind kind) {
        return kind == NodeKind.RACK || kind == NodeKind.STATION || kind == NodeKind.GARAGE
                || kind == NodeKind.FLUID_RACK || kind == NodeKind.FLUID_STATION;
    }

    /**
     * @param from the node being asked
     * @param side the side of {@code from} that faces {@code to}
     * @param to   the neighbour
     */
    public static boolean accepts(NodeKind from, Side side, NodeKind to) {
        boolean vertical = side == Side.UP || side == Side.DOWN;
        return switch (from) {
            case STOCK -> to == NodeKind.STOCK || to == NodeKind.RAIL || to == NodeKind.ELEVATOR && !vertical;
            case STATION, GARAGE, FLUID_STATION -> to == NodeKind.RAIL;
            case RACK -> to == NodeKind.RAIL || to == NodeKind.RACK;
            case FLUID_RACK -> to == NodeKind.RAIL || to == NodeKind.FLUID_RACK;
            case RAIL -> to == NodeKind.STOCK || to == NodeKind.STATION || to == NodeKind.GARAGE || to == NodeKind.RACK
                    || to == NodeKind.FLUID_RACK || to == NodeKind.FLUID_STATION || !vertical;
            case ELEVATOR -> switch (to) {
                case STATION, GARAGE, RACK, FLUID_RACK, FLUID_STATION -> false;
                case STOCK, RAIL -> !vertical;
                case ELEVATOR -> vertical;
            };
        };
    }
}
