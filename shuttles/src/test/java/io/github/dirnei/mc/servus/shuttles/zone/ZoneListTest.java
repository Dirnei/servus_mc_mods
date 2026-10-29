package io.github.dirnei.mc.servus.shuttles.zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZoneListTest {

    private static final UUID TEAM = new UUID(0, 1);
    private static final UUID OTHER = new UUID(0, 2);

    private final ZoneList<String> list = new ZoneList<>();

    @Test
    void nameTooShortIsRefused() {
        assertEquals(Optional.of(ZoneList.NameProblem.EMPTY), list.nameProblem(TEAM, "   ", ZoneList.NONE));
        assertTrue(list.create(TEAM, "", 0, "").isEmpty());
    }

    @Test
    void nameTooLongIsRefused() {
        String name = "x".repeat(ZoneList.MAX_NAME + 1);
        assertEquals(Optional.of(ZoneList.NameProblem.TOO_LONG), list.nameProblem(TEAM, name, ZoneList.NONE));
        assertTrue(list.create(TEAM, "x".repeat(ZoneList.MAX_NAME), 0, "").isPresent());
    }

    @Test
    void nameTakenIgnoringCase() {
        list.create(TEAM, "Ores", 1, "");
        assertEquals(Optional.of(ZoneList.NameProblem.TAKEN), list.nameProblem(TEAM, " ORES ", ZoneList.NONE));
        assertTrue(list.create(TEAM, "ORES", 1, "").isEmpty());
    }

    @Test
    void sameNameInAnotherTeamIsAllowed() {
        list.create(TEAM, "Ores", 1, "");
        assertTrue(list.create(OTHER, "Ores", 1, "").isPresent());
    }

    @Test
    void editKeepsItsOwnNameAndId() {
        int id = list.create(TEAM, "Ores", 1, "a").orElseThrow().id();
        ZoneList.Zone<String> edited = list.edit(id, "ores", 4, "b").orElseThrow();
        assertEquals(id, edited.id());
        assertEquals("ores", edited.name());
        assertEquals("b", edited.filter());
    }

    @Test
    void colourOutOfRangeIsRefused() {
        assertTrue(list.create(TEAM, "Ores", ZoneList.COLOURS, "").isEmpty());
        assertTrue(list.create(TEAM, "Ores", -1, "").isEmpty());
    }

    @Test
    void ordersByName() {
        list.create(TEAM, "food", 0, "");
        list.create(TEAM, "Bulk", 0, "");
        list.create(TEAM, "ores", 0, "");
        list.create(OTHER, "Apples", 0, "");
        assertEquals(List.of("Bulk", "food", "ores"), list.forTeam(TEAM).stream().map(ZoneList.Zone::name).toList());
    }

    @Test
    void searchesByText() {
        list.create(TEAM, "Raw ores", 0, "");
        list.create(TEAM, "Ores back", 0, "");
        list.create(TEAM, "Food", 0, "");
        assertEquals(List.of("Ores back", "Raw ores"), list.matching(TEAM, "ORE").stream().map(ZoneList.Zone::name).toList());
    }

    @Test
    void deletedIdResolvesToNoZone() {
        int id = list.create(TEAM, "Bulk", 0, "").orElseThrow().id();
        assertEquals(id, list.resolve(id));
        assertTrue(list.delete(id));
        assertEquals(ZoneList.NONE, list.resolve(id));
        assertNull(list.get(id));
        assertEquals(ZoneList.NONE, list.resolve(999));
    }

    @Test
    void idsAreNeverReused() {
        int first = list.create(TEAM, "Bulk", 0, "").orElseThrow().id();
        list.delete(first);
        int second = list.create(TEAM, "Bulk", 0, "").orElseThrow().id();
        assertTrue(second > first);
        ZoneList<String> loaded = new ZoneList<>(list.all(), 0);
        assertTrue(loaded.nextId() > second);
    }
}
