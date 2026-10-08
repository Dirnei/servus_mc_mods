package io.github.dirnei.mc.servus.storage.index;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Which positions around a warehouse may hold a foreign inventory worth
 * indexing: the neighbours of the stock holding members that are not members
 * themselves. Connector members such as rails contribute no neighbours.
 */
public final class CandidateNeighbours {

    private CandidateNeighbours() {
    }

    public static Set<NodePos> compute(Set<NodePos> members, Set<NodePos> connectors) {
        Set<NodePos> wanted = new LinkedHashSet<>();
        for (NodePos member : members) {
            if (connectors.contains(member)) {
                continue;
            }
            for (Side side : Side.ALL) {
                NodePos neighbour = member.relative(side);
                if (!members.contains(neighbour)) {
                    wanted.add(neighbour);
                }
            }
        }
        return wanted;
    }
}
