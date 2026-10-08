package io.github.dirnei.mc.servus.foundation.data.packet;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.List;

/**
 * One packet on its way: the route from the source position over the data
 * nodes to the target position, and how far along it is in blocks. Pure, no
 * Minecraft imports.
 */
public final class PacketFlight {

    private final List<NodePos> route;
    private double progress;

    /** @param route source position, the data nodes in order, target position; at least two entries */
    public PacketFlight(List<NodePos> route) {
        if (route.size() < 2) {
            throw new IllegalArgumentException("A route needs a start and an end");
        }
        this.route = List.copyOf(route);
    }

    public List<NodePos> route() {
        return route;
    }

    /** Length of the route in blocks: one per step between consecutive positions. */
    public int length() {
        return route.size() - 1;
    }

    public double progress() {
        return progress;
    }

    public void advance(double blocks) {
        progress = Math.min(length(), progress + blocks);
    }

    public boolean arrived() {
        return progress >= length();
    }

    /** The position the packet is at or has last passed. */
    public NodePos current() {
        return route.get((int) Math.min(length(), Math.floor(progress)));
    }

    public NodePos source() {
        return route.get(0);
    }

    public NodePos target() {
        return route.get(route.size() - 1);
    }

    /** The data node the packet would leave the network from, next to the target. */
    public NodePos lastNode() {
        return route.get(route.size() - 2);
    }

    public boolean passes(NodePos pos) {
        return route.subList((int) Math.floor(Math.min(length(), progress)), route.size()).contains(pos);
    }

    /** Ticks a whole route takes at a speed in blocks per second. */
    public static int ticksFor(int blocks, double blocksPerSecond) {
        return (int) Math.ceil(blocks * 20.0 / blocksPerSecond - 1e-9);
    }
}
