package io.github.dirnei.mc.servus.notes.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * One task of a list. Subtasks are kept flat in list order with {@code sub} set: a subtask belongs
 * to the nearest task above it that is not a subtask, so tasks nest one level by construction.
 *
 * @param notes free text shown below the task, empty when there are none
 * @param goals the item goals in the order they were added
 * @param spot  where the work is, if the task names a place
 */
public record Task(UUID id, String text, String notes, boolean sub, Optional<Person> assignee, Optional<DoneRecord> done,
                   List<Goal> goals, Optional<Spot> spot) {

    public Task {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(assignee, "assignee");
        Objects.requireNonNull(done, "done");
        Objects.requireNonNull(spot, "spot");
        goals = List.copyOf(goals);
    }

    /** A task without goals or spot. */
    public Task(UUID id, String text, String notes, boolean sub, Optional<Person> assignee, Optional<DoneRecord> done) {
        this(id, text, notes, sub, assignee, done, List.of(), Optional.empty());
    }

    /** A new open task without notes or assignee. */
    public static Task open(UUID id, String text) {
        return new Task(id, text, "", false, Optional.empty(), Optional.empty());
    }

    public boolean isDone() {
        return done.isPresent();
    }

    public Optional<Goal> goal(UUID goalId) {
        return goals.stream().filter(goal -> goal.id().equals(goalId)).findFirst();
    }

    public Task withText(String newText) {
        return new Task(id, newText, notes, sub, assignee, done, goals, spot);
    }

    public Task withNotes(String newNotes) {
        return new Task(id, text, newNotes, sub, assignee, done, goals, spot);
    }

    public Task withSub(boolean newSub) {
        return new Task(id, text, notes, newSub, assignee, done, goals, spot);
    }

    public Task withAssignee(Optional<Person> newAssignee) {
        return new Task(id, text, notes, sub, newAssignee, done, goals, spot);
    }

    public Task withDone(Optional<DoneRecord> newDone) {
        return new Task(id, text, notes, sub, assignee, newDone, goals, spot);
    }

    public Task withGoals(List<Goal> newGoals) {
        return new Task(id, text, notes, sub, assignee, done, newGoals, spot);
    }

    public Task withSpot(Optional<Spot> newSpot) {
        return new Task(id, text, notes, sub, assignee, done, goals, newSpot);
    }
}
