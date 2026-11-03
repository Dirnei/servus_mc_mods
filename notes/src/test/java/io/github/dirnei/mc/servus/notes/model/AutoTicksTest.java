package io.github.dirnei.mc.servus.notes.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class AutoTicksTest {

    private static final Person ANNA = new Person(new UUID(0, 1), "Anna");
    private static final Person BEN = new Person(new UUID(0, 2), "Ben");

    private static int ids;

    @Test
    void ownerOfAPrivateListClaimsUnassignedTasks() {
        Task task = task(goal("minecraft:stone", 0, 64));
        NoteList list = list(Scope.PRIVATE, task);

        assertEquals(List.of(task.id()), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 64))));
        assertEquals(List.of(), AutoTicks.due(list, BEN.id(), carries(Map.of("minecraft:stone", 64))));
    }

    @Test
    void unassignedTasksOnSharedListsAreNobodys() {
        NoteList list = list(Scope.SERVER, task(goal("minecraft:stone", 0, 64)));

        assertEquals(List.of(), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 64))));
    }

    @Test
    void assigneeClaimsTheTask() {
        Task task = task(goal("minecraft:stone", 0, 64)).withAssignee(Optional.of(BEN));
        NoteList list = list(Scope.SERVER, task);

        assertEquals(List.of(task.id()), AutoTicks.due(list, BEN.id(), carries(Map.of("minecraft:stone", 64))));
        assertEquals(List.of(), AutoTicks.due(list(Scope.PRIVATE, task), ANNA.id(), carries(Map.of("minecraft:stone", 64))));
    }

    @Test
    void carriedItemsAddToTheSharedCount() {
        Task task = task(goal("minecraft:stone", 40, 64));
        NoteList list = list(Scope.PRIVATE, task);

        assertEquals(List.of(task.id()), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 24))));
        assertEquals(List.of(), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 23))));
    }

    @Test
    void everyGoalMustBeCoveredAndSameItemGoalsAddUp() {
        Task task = task(goal("minecraft:stone", 0, 32), goal("minecraft:stone", 0, 32), goal("minecraft:torch", 0, 8));
        NoteList list = list(Scope.PRIVATE, task);

        assertEquals(List.of(), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 32, "minecraft:torch", 8))));
        assertEquals(List.of(), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 64))));
        assertEquals(List.of(task.id()), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 64, "minecraft:torch", 8))));
    }

    @Test
    void doneTasksAndTasksWithoutGoalsAreSkipped() {
        Task done = task(goal("minecraft:stone", 0, 1)).withDone(Optional.of(new DoneRecord(ANNA, 1, 1)));
        NoteList list = list(Scope.PRIVATE, done, task());

        assertEquals(List.of(), AutoTicks.due(list, ANNA.id(), carries(Map.of("minecraft:stone", 64))));
    }

    private static ToIntFunction<String> carries(Map<String, Integer> items) {
        return id -> items.getOrDefault(id, 0);
    }

    private static NoteList list(Scope scope, Task... tasks) {
        return new NoteList(new UUID(1, 1), "Base", ANNA.id(), ANNA.name(), List.of(tasks), scope, Optional.empty(), List.of());
    }

    private static Task task(Goal... goals) {
        return Task.open(new UUID(2, ++ids), "Task").withGoals(List.of(goals));
    }

    private static Goal goal(String itemId, int count, int target) {
        return new Goal(new UUID(3, ++ids), itemId, target, count, ANNA);
    }
}
