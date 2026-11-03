package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class PinLinesTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");
    private static final Function<String, String> NAMES = id -> id.substring(id.indexOf(':') + 1);
    private static final ToIntFunction<String> CARRIED = id -> id.equals("minecraft:torch") ? 40 : 2;

    private static int ids;

    @Test
    void openTasksInOrderWithSubtasksIndented() {
        NoteList list = list(open("Portal"), open("Obsidian").withSub(true), open("Road"));

        List<PinLines.Line> lines = PinLines.of(list, 4, NAMES, CARRIED);

        assertEquals(List.of(new PinLines.TaskLine("Portal", false), new PinLines.TaskLine("Obsidian", true),
                new PinLines.TaskLine("Road", false)), lines);
        assertEquals("  [ ] Obsidian", ((PinLines.TaskLine) lines.get(1)).label());
        assertEquals("[ ] Portal", ((PinLines.TaskLine) lines.get(0)).label());
    }

    @Test
    void doneTasksAndTheirGoalsDropOff() {
        Task done = open("Walls").withDone(Optional.of(new DoneRecord(ANNA, 1, 1)))
                .withGoals(List.of(goal("minecraft:stone", 3, 64)));
        NoteList list = list(open("Portal"), done);

        assertEquals(List.of(new PinLines.TaskLine("Portal", false)), PinLines.of(list, 4, NAMES, CARRIED));
    }

    @Test
    void goalsComeAfterTasksAndReachedGoalsDropOff() {
        Task first = open("Portal").withGoals(List.of(goal("minecraft:obsidian", 4, 10), goal("minecraft:flint", 1, 1)));
        Task second = open("Road").withGoals(List.of(goal("minecraft:cobblestone", 70, 64), goal("minecraft:torch", 0, 32)));
        NoteList list = list(first, second);

        List<PinLines.Line> lines = PinLines.of(list, 8, NAMES, CARRIED);

        assertEquals(List.of(new PinLines.TaskLine("Portal", false), new PinLines.TaskLine("Road", false),
                new PinLines.GoalLine("obsidian", 4, 10, 2), new PinLines.GoalLine("torch", 0, 32, 40)), lines);
        assertEquals(new GoalFill(0.4f, 0.2f), ((PinLines.GoalLine) lines.get(2)).fill());
        assertEquals(new GoalFill(0f, 1f), ((PinLines.GoalLine) lines.get(3)).fill());
    }

    @Test
    void cutLinesEndWithMore() {
        NoteList list = list(open("a"), open("b"), open("c"), open("d"), open("e"), open("f"));

        List<PinLines.Line> lines = PinLines.of(list, 4, NAMES, CARRIED);

        assertEquals(5, lines.size());
        assertEquals(new PinLines.TaskLine("d", false), lines.get(3));
        assertEquals(new PinLines.More(2), lines.get(4));
    }

    @Test
    void exactlyTheLineCountNeedsNoMore() {
        NoteList list = list(open("a"), open("b"));

        assertEquals(2, PinLines.of(list, 2, NAMES, CARRIED).size());
    }

    private static NoteList list(Task... tasks) {
        return new NoteList(new UUID(1, 1), "Nether", ANNA.id(), ANNA.name(), List.of(tasks));
    }

    private static Task open(String text) {
        return Task.open(new UUID(2, ++ids), text);
    }

    private static Goal goal(String item, int count, int target) {
        return new Goal(new UUID(3, ++ids), item, target, count, ANNA);
    }
}
