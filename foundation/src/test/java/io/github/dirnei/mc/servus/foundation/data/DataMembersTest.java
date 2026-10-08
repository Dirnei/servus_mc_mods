package io.github.dirnei.mc.servus.foundation.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DataMembersTest {

    private static final NodePos CABLE_A = new NodePos(0, 0, 0);
    private static final NodePos CABLE_B = new NodePos(2, 0, 0);
    private static final NodePos MEMBER = new NodePos(1, 0, 0);
    private static final NodePos OTHER = new NodePos(0, 1, 0);

    @Test
    void oneTouchMakesAMember() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.PRODUCER));
        assertEquals(1, members.size());
        assertEquals(EnumSet.of(DataRole.PRODUCER), members.all().get(MEMBER));
    }

    @Test
    void twoTouchesOfOneMemberCountOnce() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.STOCK));
        members.touch(CABLE_B, Side.WEST, MEMBER, Set.of(DataRole.STOCK));
        assertEquals(1, members.size());
    }

    @Test
    void rolesAreTheUnionOfTouches() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.STOCK));
        members.touch(CABLE_B, Side.WEST, MEMBER, Set.of(DataRole.PRODUCER));
        assertEquals(EnumSet.allOf(DataRole.class), members.all().get(MEMBER));
    }

    @Test
    void lastTouchRemovesTheMember() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.STOCK));
        members.touch(CABLE_B, Side.WEST, MEMBER, Set.of(DataRole.STOCK));
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of());
        assertTrue(members.contains(MEMBER));
        members.touch(CABLE_B, Side.WEST, MEMBER, Set.of());
        assertFalse(members.contains(MEMBER));
        assertEquals(0, members.size());
    }

    @Test
    void removingACableDropsAllItsTouches() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.PRODUCER));
        members.touch(CABLE_A, Side.UP, OTHER, Set.of(DataRole.STOCK));
        members.touch(CABLE_B, Side.WEST, MEMBER, Set.of(DataRole.STOCK));
        members.removeCable(CABLE_A);
        assertFalse(members.contains(OTHER));
        assertEquals(EnumSet.of(DataRole.STOCK), members.all().get(MEMBER));
    }

    @Test
    void membersByRole() {
        DataMembers members = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.PRODUCER));
        members.touch(CABLE_A, Side.UP, OTHER, Set.of(DataRole.STOCK));
        assertEquals(Set.of(MEMBER), members.withRole(DataRole.PRODUCER));
        assertEquals(Set.of(OTHER), members.withRole(DataRole.STOCK));
    }

    @Test
    void absorbTakesOverTouches() {
        DataMembers members = new DataMembers();
        DataMembers other = new DataMembers();
        members.touch(CABLE_A, Side.EAST, MEMBER, Set.of(DataRole.PRODUCER));
        other.touch(CABLE_B, Side.WEST, MEMBER, Set.of(DataRole.STOCK));
        other.touch(CABLE_B, Side.UP, OTHER, Set.of(DataRole.STOCK));
        members.absorb(other);
        assertEquals(2, members.size());
        assertEquals(EnumSet.allOf(DataRole.class), members.all().get(MEMBER));
    }
}
