package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Why a shuttle drives or stands this tick, as the server sees it. A shuttle
 * held behind another names that one's id; one waiting for a lift car or a
 * deck names the shaft's lowest segment and the floor it waits on, counted
 * from the lowest segment as floor 1. Pure Java.
 *
 * @param leader the id of the shuttle it waits behind, -1 when none
 * @param shaft  the lowest segment of the shaft it waits at, null when none
 * @param floor  the floor it waits on, 0 when none
 */
public record StandReason(Kind kind, int leader, NodePos shaft, int floor) {

    /** What a shuttle does now; the order is the sync id, append only. */
    public enum Kind {
        DRIVING, FOLLOWING, RING, STOP, FIELD_WORK, DOOR, HANDOVER, RIDING, LIFT, DECK, POWER, CHUNK;

        private static final Kind[] VALUES = values();

        public static Kind byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : DRIVING;
        }
    }

    /** The walk along the shuttles each one waits behind: their ids in order and whether it came back to one already named. */
    public record Chain(List<Integer> ids, boolean cycle) {
    }

    public static final StandReason DRIVING = of(Kind.DRIVING);

    /** A reason that names no shuttle and no shaft. */
    public static StandReason of(Kind kind) {
        return new StandReason(kind, -1, null, 0);
    }

    /** Held behind the shuttle with that id. */
    public static StandReason following(int leader) {
        return new StandReason(Kind.FOLLOWING, leader, null, 0);
    }

    /** Waiting at the shaft whose lowest segment is {@code bottom} for the car ({@link Kind#LIFT}) or a deck ({@link Kind#DECK}). */
    public static StandReason atShaft(Kind kind, NodePos bottom, int y) {
        return new StandReason(kind, -1, bottom, floor(y, bottom.y()));
    }

    /** The floor of the segment at height y in a shaft whose lowest segment is at bottomY: 1 for the lowest. */
    public static int floor(int y, int bottomY) {
        return y - bottomY + 1;
    }

    /**
     * Follows the shuttles each one waits behind, starting with the one
     * {@code start} waits behind, until one waits behind none, the walk comes
     * back to a shuttle already named (start included) or it has named
     * {@code limit} shuttles.
     *
     * @param leaderOf the id a shuttle waits behind, -1 when it waits behind none
     */
    public static Chain chain(int start, IntUnaryOperator leaderOf, int limit) {
        List<Integer> ids = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        seen.add(start);
        int next = leaderOf.applyAsInt(start);
        while (next >= 0 && ids.size() < limit) {
            if (!seen.add(next)) {
                return new Chain(ids, true);
            }
            ids.add(next);
            next = leaderOf.applyAsInt(next);
        }
        return new Chain(ids, false);
    }
}
