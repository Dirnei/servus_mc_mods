package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.lib.graph.NodePos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Groups elevator segments into shafts: maximal runs of stacked segments in one column. Pure, no Minecraft imports. */
public final class Shafts {

    private Shafts() {
    }

    /** A shaft: its lowest segment and the y of its top segment. */
    public record Shaft(NodePos bottom, int topY) {

        public int bottomY() {
            return bottom.y();
        }

        /** Whether the node is one of this shaft's segments. */
        public boolean contains(NodePos node) {
            return node.x() == bottom.x() && node.z() == bottom.z() && node.y() >= bottom.y() && node.y() <= topY;
        }

        /** Number of segments, one per floor. */
        public int height() {
            return topY - bottom.y() + 1;
        }
    }

    /** Groups elevator nodes by column into maximal runs of consecutive y, ordered by bottom. */
    public static List<Shaft> group(Set<NodePos> elevators) {
        List<NodePos> sorted = new ArrayList<>(elevators);
        sorted.sort(Comparator.comparingInt(NodePos::x).thenComparingInt(NodePos::z).thenComparingInt(NodePos::y));
        List<Shaft> shafts = new ArrayList<>();
        NodePos bottom = null;
        NodePos last = null;
        for (NodePos node : sorted) {
            boolean continues = last != null && last.x() == node.x() && last.z() == node.z() && last.y() + 1 == node.y();
            if (!continues) {
                if (bottom != null) {
                    shafts.add(new Shaft(bottom, last.y()));
                }
                bottom = node;
            }
            last = node;
        }
        if (bottom != null) {
            shafts.add(new Shaft(bottom, last.y()));
        }
        return shafts;
    }

    /** Every segment of the shafts mapped to its shaft, as a lookup for path steps. */
    public static Map<NodePos, Shaft> floorsOf(Collection<Shaft> shafts) {
        Map<NodePos, Shaft> floors = new HashMap<>();
        for (Shaft shaft : shafts) {
            for (int y = shaft.bottomY(); y <= shaft.topY(); y++) {
                floors.put(new NodePos(shaft.bottom().x(), y, shaft.bottom().z()), shaft);
            }
        }
        return floors;
    }
}
