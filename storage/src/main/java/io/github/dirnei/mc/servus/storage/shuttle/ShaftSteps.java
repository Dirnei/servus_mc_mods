package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import io.github.dirnei.mc.servus.storage.shuttle.Shafts.Shaft;
import java.util.List;
import java.util.function.Function;

/** Classifies the steps of a shuttle path by how they meet elevator shafts. Pure, no Minecraft imports. */
public final class ShaftSteps {

    /** What a step from node i to node i + 1 does. */
    public enum Kind {
        /** Rail to rail, nothing to do with a shaft. */
        PLAIN,
        /** Into a shaft whose car the shuttle then rides, or ends its way on. */
        ENTER_RIDE,
        /** Into a shaft that the way leaves sideways on the same floor. */
        ENTER_CROSS,
        /** Segment to segment of one shaft: up or down on the car. */
        RIDE,
        /** Out of a shaft onto a rail beside it. */
        EXIT
    }

    private ShaftSteps() {
    }

    /**
     * @param shaftOf the shaft a node is a segment of, or null for rails
     * @return the kind of step i, from node i to node i + 1
     */
    public static Kind classify(List<NodePos> path, int i, Function<NodePos, Shaft> shaftOf) {
        Shaft from = shaftOf.apply(path.get(i));
        Shaft to = shaftOf.apply(path.get(i + 1));
        if (to != null && !to.equals(from)) {
            if (i + 2 >= path.size() || vertical(path.get(i + 1), path.get(i + 2))) {
                return Kind.ENTER_RIDE;
            }
            return Kind.ENTER_CROSS;
        }
        if (to != null) {
            return Kind.RIDE;
        }
        return from != null ? Kind.EXIT : Kind.PLAIN;
    }

    /** Index of the last node of the vertical run that starts at node i. */
    public static int rideEnd(List<NodePos> path, int i) {
        NodePos start = path.get(i);
        int j = i;
        while (j + 1 < path.size() && path.get(j + 1).x() == start.x() && path.get(j + 1).z() == start.z()) {
            j++;
        }
        return j;
    }

    private static boolean vertical(NodePos a, NodePos b) {
        return a.x() == b.x() && a.z() == b.z() && a.y() != b.y();
    }
}
