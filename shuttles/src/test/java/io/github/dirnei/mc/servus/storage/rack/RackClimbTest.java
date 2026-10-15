package io.github.dirnei.mc.servus.storage.rack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.storage.rack.RackClimb.Box;
import io.github.dirnei.mc.servus.storage.rack.RackClimb.Contact;
import java.util.List;
import org.junit.jupiter.api.Test;

class RackClimbTest {

    private static final double TOL = 1.0E-3;
    private static final double GRAVITY = 0.08;

    /** A player sized box: 0.6 wide, 1.8 tall. */
    private static Box player(double minX, double minY, double minZ) {
        return new Box(minX, minY, minZ, minX + 0.6, minY + 1.8, minZ + 0.6);
    }

    @Test
    void westFaceListsTheColumnBeyondIt() {
        List<Contact> contacts = RackClimb.contacts(player(5.0, 64.0, 3.2), TOL);
        assertEquals(List.of(new Contact(4, 64, 3, -1, 0), new Contact(4, 65, 3, -1, 0)), contacts);
    }

    @Test
    void eastFaceListsTheColumnBeyondIt() {
        List<Contact> contacts = RackClimb.contacts(player(4.4, 64.0, 3.2), TOL);
        assertEquals(List.of(new Contact(5, 64, 3, 1, 0), new Contact(5, 65, 3, 1, 0)), contacts);
    }

    @Test
    void boxStraddlingTwoColumnsListsBoth() {
        List<Contact> contacts = RackClimb.contacts(player(5.0, 64.0, 3.7), TOL);
        assertEquals(List.of(
                new Contact(4, 64, 3, -1, 0), new Contact(4, 65, 3, -1, 0),
                new Contact(4, 64, 4, -1, 0), new Contact(4, 65, 4, -1, 0)), contacts);
    }

    @Test
    void boxBetweenBoundariesListsThreeRows() {
        List<Contact> contacts = RackClimb.contacts(player(5.0, 64.5, 3.2), TOL);
        assertEquals(List.of(new Contact(4, 64, 3, -1, 0), new Contact(4, 65, 3, -1, 0), new Contact(4, 66, 3, -1, 0)), contacts);
    }

    @Test
    void boxOnNoBoundaryListsNothing() {
        assertTrue(RackClimb.contacts(player(5.2, 64.0, 3.2), TOL).isEmpty());
    }

    @Test
    void cornerListsBothFaces() {
        List<Contact> contacts = RackClimb.contacts(player(5.0, 64.0, 3.0), TOL);
        assertEquals(List.of(
                new Contact(4, 64, 3, -1, 0), new Contact(4, 65, 3, -1, 0),
                new Contact(5, 64, 2, 0, -1), new Contact(5, 65, 2, 0, -1)), contacts);
    }

    @Test
    void southFaceListsTheRowBeyondIt() {
        List<Contact> contacts = RackClimb.contacts(player(5.2, 64.0, 3.4), TOL);
        assertEquals(List.of(new Contact(5, 64, 4, 0, 1), new Contact(5, 65, 4, 0, 1)), contacts);
    }

    @Test
    void sidesAreAtARightAngleToTheFacing() {
        // Rack faces north: step (0, -1).
        assertTrue(RackClimb.isSide(0, -1, 1, 0), "east step touches the west side");
        assertTrue(RackClimb.isSide(0, -1, -1, 0), "west step touches the east side");
        assertFalse(RackClimb.isSide(0, -1, 0, 1), "south step touches the front");
        assertFalse(RackClimb.isSide(0, -1, 0, -1), "north step touches the back");
        // Rack faces east: step (1, 0).
        assertTrue(RackClimb.isSide(1, 0, 0, 1));
        assertFalse(RackClimb.isSide(1, 0, -1, 0));
    }

    @Test
    void pushingClimbsAtVanillaSpeed() {
        assertEquals((0.2 - GRAVITY) * 0.98, RackClimb.verticalMotion(-0.3, true, false, GRAVITY), 1.0E-9);
        assertEquals((0.2 - GRAVITY) * 0.98, RackClimb.verticalMotion(0.5, true, true, GRAVITY), 1.0E-9);
    }

    @Test
    void hangingSlidesSlowly() {
        assertEquals(-0.15, RackClimb.verticalMotion(-0.5, false, false, GRAVITY), 1.0E-9);
        assertEquals(-0.1, RackClimb.verticalMotion(-0.1, false, false, GRAVITY), 1.0E-9);
    }

    @Test
    void sneakingHolds() {
        assertEquals(0.0, RackClimb.verticalMotion(-0.5, false, true, GRAVITY), 1.0E-9);
    }

    @Test
    void risingIsLeftAlone() {
        assertEquals(0.05, RackClimb.verticalMotion(0.05, false, false, GRAVITY), 1.0E-9);
        assertEquals(0.05, RackClimb.verticalMotion(0.05, false, true, GRAVITY), 1.0E-9);
    }
}
