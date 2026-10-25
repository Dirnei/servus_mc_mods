package io.github.dirnei.mc.servus.shuttles.shuttle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StandReasonTest {

    private static StandReason.Chain chain(int start, Map<Integer, Integer> leaders, int limit) {
        return StandReason.chain(start, id -> leaders.getOrDefault(id, -1), limit);
    }

    @Test
    void chainOfThreeEndsAtTheShuttleThatWaitsBehindNone() {
        StandReason.Chain chain = chain(3, Map.of(3, 5, 5, 7), 8);
        assertEquals(List.of(5, 7), chain.ids());
        assertFalse(chain.cycle());
    }

    @Test
    void chainStopsAtTheLimit() {
        StandReason.Chain chain = chain(1, Map.of(1, 2, 2, 3, 3, 4, 4, 5), 2);
        assertEquals(List.of(2, 3), chain.ids());
        assertFalse(chain.cycle());
    }

    @Test
    void twoShuttlesWaitingForEachOtherCloseACycle() {
        StandReason.Chain chain = chain(3, Map.of(3, 5, 5, 3), 8);
        assertEquals(List.of(5), chain.ids());
        assertTrue(chain.cycle());
    }

    @Test
    void cycleFurtherAlongTheChain() {
        StandReason.Chain chain = chain(1, Map.of(1, 2, 2, 3, 3, 2), 8);
        assertEquals(List.of(2, 3), chain.ids());
        assertTrue(chain.cycle());
    }

    @Test
    void shuttleWithoutLeaderHasAnEmptyChain() {
        StandReason.Chain chain = chain(4, Map.of(), 8);
        assertTrue(chain.ids().isEmpty());
        assertFalse(chain.cycle());
    }

    @Test
    void floorsCountFromTheLowestSegment() {
        assertEquals(1, StandReason.floor(64, 64));
        assertEquals(3, StandReason.floor(66, 64));
        StandReason lift = StandReason.atShaft(StandReason.Kind.LIFT, new NodePos(0, 10, 0), 11);
        assertEquals(2, lift.floor());
        assertEquals(StandReason.Kind.LIFT, lift.kind());
    }

    @Test
    void unknownSyncIdReadsAsDriving() {
        assertEquals(StandReason.Kind.DRIVING, StandReason.Kind.byId(-1));
        assertEquals(StandReason.Kind.DRIVING, StandReason.Kind.byId(99));
        assertEquals(StandReason.Kind.CHUNK, StandReason.Kind.byId(StandReason.Kind.CHUNK.ordinal()));
    }
}
