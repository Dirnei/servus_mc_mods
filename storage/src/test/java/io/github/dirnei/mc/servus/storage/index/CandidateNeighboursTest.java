package io.github.dirnei.mc.servus.storage.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CandidateNeighboursTest {

    private static final NodePos A = new NodePos(0, 0, 0);
    private static final NodePos B = new NodePos(1, 0, 0);
    private static final NodePos C = new NodePos(2, 0, 0);

    @Test
    void singleMemberHasSixNeighbours() {
        Set<NodePos> wanted = CandidateNeighbours.compute(Set.of(A), Set.of());
        assertEquals(6, wanted.size());
        assertTrue(wanted.contains(new NodePos(0, 1, 0)));
        assertFalse(wanted.contains(A));
    }

    @Test
    void touchingMembersShareNeighboursAndExcludeEachOther() {
        Set<NodePos> wanted = CandidateNeighbours.compute(Set.of(A, B), Set.of());
        assertEquals(10, wanted.size());
        assertFalse(wanted.contains(A));
        assertFalse(wanted.contains(B));
        assertTrue(wanted.contains(new NodePos(0, 0, 1)));
        assertTrue(wanted.contains(new NodePos(1, 0, 1)));
    }

    @Test
    void connectorBetweenMembersAddsNoNeighbours() {
        Set<NodePos> wanted = CandidateNeighbours.compute(Set.of(A, B, C), Set.of(B));
        assertFalse(wanted.contains(new NodePos(1, 0, 1)), "the rail's own side neighbour is not a candidate");
        assertFalse(wanted.contains(new NodePos(1, 1, 0)), "the block above the rail is not a candidate");
        assertTrue(wanted.contains(new NodePos(0, 0, 1)));
        assertTrue(wanted.contains(new NodePos(2, 0, 1)));
        assertFalse(wanted.contains(B));
        assertEquals(10, wanted.size());
    }

    @Test
    void connectorOnlySetHasNoCandidates() {
        Set<NodePos> wanted = CandidateNeighbours.compute(Set.of(A, B), Set.of(A, B));
        assertTrue(wanted.isEmpty());
    }
}
