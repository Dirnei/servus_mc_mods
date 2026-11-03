package io.github.dirnei.mc.servus.notes.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Finds the tasks another player ticked between two snapshots of the same list. */
public final class NewTicks {

    /** A task that became done, and who ticked it. */
    public record Tick(Task task, Person by) {
    }

    private NewTicks() {
    }

    /** Tasks open in {@code before} and done in {@code after} by someone other than {@code self}, in list order. */
    public static List<Tick> between(NoteList before, NoteList after, UUID self) {
        List<Tick> ticks = new ArrayList<>();
        for (Task task : after.tasks()) {
            Optional<DoneRecord> done = task.done();
            if (done.isEmpty() || done.get().by().id().equals(self)) {
                continue;
            }
            if (before.task(task.id()).map(old -> !old.isDone()).orElse(false)) {
                ticks.add(new Tick(task, done.get().by()));
            }
        }
        return List.copyOf(ticks);
    }
}
