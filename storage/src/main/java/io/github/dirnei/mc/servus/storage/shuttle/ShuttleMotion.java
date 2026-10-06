package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A shuttle travelling along a node path: which segment it is on and how far
 * along it. Pure, no Minecraft imports.
 */
public final class ShuttleMotion {

    private final List<NodePos> path;
    private int segment;
    private double progress;

    /** @param path at least one node; a one node path has arrived already */
    public ShuttleMotion(List<NodePos> path) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path must not be empty");
        }
        this.path = List.copyOf(path);
    }

    public static ShuttleMotion parked(NodePos at) {
        return new ShuttleMotion(List.of(at));
    }

    public List<NodePos> path() {
        return path;
    }

    public int segment() {
        return segment;
    }

    public double progress() {
        return progress;
    }

    public NodePos start() {
        return path.get(0);
    }

    public NodePos end() {
        return path.get(path.size() - 1);
    }

    public boolean arrived() {
        return segment >= path.size() - 1;
    }

    /** The node the shuttle is on or has last passed. */
    public NodePos currentNode() {
        return arrived() ? end() : path.get(segment);
    }

    /**
     * Moves the shuttle along the path.
     *
     * @return true when the end of the path has been reached by this call or earlier
     */
    public boolean advance(double blocks) {
        double left = blocks;
        while (left > 0 && !arrived()) {
            double room = 1.0 - progress;
            if (left >= room) {
                left -= room;
                segment++;
                progress = 0;
            } else {
                progress += left;
                left = 0;
            }
        }
        return arrived();
    }

    /** Interpolated position as x, y, z block coordinates of the node corner. */
    public double[] position() {
        if (arrived()) {
            NodePos end = end();
            return new double[] {end.x(), end.y(), end.z()};
        }
        NodePos from = path.get(segment);
        NodePos to = path.get(segment + 1);
        return new double[] {
                from.x() + (to.x() - from.x()) * progress,
                from.y() + (to.y() - from.y()) * progress,
                from.z() + (to.z() - from.z()) * progress};
    }

    /** The way back: the same nodes from the end to the start. */
    public ShuttleMotion reverse() {
        List<NodePos> back = new ArrayList<>(path);
        Collections.reverse(back);
        return new ShuttleMotion(back);
    }
}
