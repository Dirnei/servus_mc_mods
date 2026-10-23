package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * The step through a garage's doorway, between the garage's own cell and its
 * home rail. Only that garage's shuttles may take it, so it is added to their
 * ways alone; the garage cell leads nowhere else. Pure Java.
 */
final class GarageDoorway {

    private GarageDoorway() {
    }

    /** The ways with the doorway step added in both directions: inside to home and home to inside. */
    static Function<NodePos, Set<NodePos>> through(Function<NodePos, Set<NodePos>> ways, NodePos inside, NodePos home) {
        return node -> {
            if (node.equals(inside)) {
                return Set.of(home);
            }
            Set<NodePos> next = ways.apply(node);
            if (!node.equals(home)) {
                return next;
            }
            Set<NodePos> withDoorway = new LinkedHashSet<>(next);
            withDoorway.add(inside);
            return withDoorway;
        };
    }
}
