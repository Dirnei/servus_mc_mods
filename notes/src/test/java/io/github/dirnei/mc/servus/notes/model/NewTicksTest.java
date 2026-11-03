package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewTicksTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");
    private static final Person DIRNEI = new Person(new UUID(0, 2), "Dirnei");
    private static final Task PORTAL = Task.open(new UUID(2, 1), "Portal at spawn");
    private static final Task ROAD = Task.open(new UUID(2, 2), "Road");

    @Test
    void tickByAnotherPlayerIsNew() {
        NoteList before = list(PORTAL, ROAD);
        NoteList after = list(ticked(PORTAL, ANNA), ROAD);

        List<NewTicks.Tick> ticks = NewTicks.between(before, after, DIRNEI.id());

        assertEquals(List.of(new NewTicks.Tick(ticked(PORTAL, ANNA), ANNA)), ticks);
    }

    @Test
    void ownTickIsIgnored() {
        assertTrue(NewTicks.between(list(PORTAL), list(ticked(PORTAL, DIRNEI)), DIRNEI.id()).isEmpty());
    }

    @Test
    void untickIsIgnored() {
        assertTrue(NewTicks.between(list(ticked(PORTAL, ANNA)), list(PORTAL), DIRNEI.id()).isEmpty());
    }

    @Test
    void alreadyDoneIsIgnored() {
        NoteList before = list(ticked(PORTAL, ANNA));
        NoteList after = list(ticked(PORTAL, ANNA).withText("Portal at the spawn"));

        assertTrue(NewTicks.between(before, after, DIRNEI.id()).isEmpty());
    }

    @Test
    void taskAddedDoneIsIgnored() {
        assertTrue(NewTicks.between(list(PORTAL), list(PORTAL, ticked(ROAD, ANNA)), DIRNEI.id()).isEmpty());
    }

    private static NoteList list(Task... tasks) {
        return new NoteList(new UUID(1, 1), "Nether highway", DIRNEI.id(), DIRNEI.name(), List.of(tasks));
    }

    private static Task ticked(Task task, Person by) {
        return task.withDone(Optional.of(new DoneRecord(by, 3, 100)));
    }
}
