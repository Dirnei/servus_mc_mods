package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The ways of a shuttle too wide for a lift car: every step up or down an
 * elevator shaft is left out, so it still drives straight through a shaft on
 * its own floor but never rides. Pure Java.
 */
public final class LiftlessSteps {

    private LiftlessSteps() {
    }

    /** The neighbours of the node without those straight above or below it. */
    public static Set<NodePos> filter(NodePos node, Set<NodePos> neighbours) {
        Set<NodePos> kept = new LinkedHashSet<>();
        for (NodePos neighbour : neighbours) {
            if (neighbour.x() != node.x() || neighbour.z() != node.z()) {
                kept.add(neighbour);
            }
        }
        return kept;
    }
}
