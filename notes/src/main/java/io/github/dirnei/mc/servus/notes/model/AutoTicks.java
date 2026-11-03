package io.github.dirnei.mc.servus.notes.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/**
 * Finds the open item tasks a player has claimed and now carries everything for. A task is claimed by its
 * assignee; a task without one on a Private list is claimed by the list's owner.
 */
public final class AutoTicks {

    private AutoTicks() {
    }

    /** Whether {@code self} claims {@code task} of {@code list}. */
    public static boolean claimedBy(NoteList list, Task task, UUID self) {
        return task.assignee()
                .map(person -> person.id().equals(self))
                .orElse(list.scope() == Scope.PRIVATE && list.owner().equals(self));
    }

    /**
     * The ids of open tasks with goals that {@code self} claims and whose missing items the player carries,
     * in list order.
     *
     * @param carried how many of an item, by registry id, the player carries
     */
    public static List<UUID> due(NoteList list, UUID self, ToIntFunction<String> carried) {
        List<UUID> due = new ArrayList<>();
        for (Task task : list.tasks()) {
            if (!task.isDone() && !task.goals().isEmpty() && claimedBy(list, task, self) && covered(task, carried)) {
                due.add(task.id());
            }
        }
        return List.copyOf(due);
    }

    private static boolean covered(Task task, ToIntFunction<String> carried) {
        Map<String, Integer> missing = new HashMap<>();
        for (Goal goal : task.goals()) {
            missing.merge(goal.itemId(), Math.max(0, goal.target() - goal.count()), Integer::sum);
        }
        return missing.entrySet().stream().allMatch(entry -> carried.applyAsInt(entry.getKey()) >= entry.getValue());
    }
}
