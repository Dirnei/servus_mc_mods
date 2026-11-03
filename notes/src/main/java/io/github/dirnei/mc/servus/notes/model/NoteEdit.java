package io.github.dirnei.mc.servus.notes.model;

import java.util.Optional;
import java.util.UUID;

/**
 * One change a player asks for. New lists and tasks carry the id the client chose, so the client
 * can select what it just made; the server refuses an id that is already taken.
 */
public sealed interface NoteEdit {

    /** The list this edit is about. */
    UUID listId();

    record CreateList(UUID listId, String name) implements NoteEdit {
    }

    record RenameList(UUID listId, String name) implements NoteEdit {
    }

    record DeleteList(UUID listId) implements NoteEdit {
    }

    /** Adds a task after {@code afterId}, or at the end when it is empty or not in the list, with an optional spot. */
    record AddTask(UUID listId, UUID taskId, String text, Optional<UUID> afterId, Optional<Spot> spot) implements NoteEdit {

        /** A task without spot. */
        public AddTask(UUID listId, UUID taskId, String text, Optional<UUID> afterId) {
            this(listId, taskId, text, afterId, Optional.empty());
        }
    }

    record EditText(UUID listId, UUID taskId, String text) implements NoteEdit {
    }

    record EditNotes(UUID listId, UUID taskId, String notes) implements NoteEdit {
    }

    /** Deletes the task; a task that is not a subtask takes its subtasks with it. */
    record DeleteTask(UUID listId, UUID taskId) implements NoteEdit {
    }

    /**
     * Moves a task to just after {@code afterId}, or to the top when it is empty, as a subtask ({@code sub}) of the
     * task above or as a task. A task with subtasks moves with them and stays a task; a task placed as a task after a
     * subtask lands behind that subtask's whole parent block.
     */
    record PlaceTask(UUID listId, UUID taskId, Optional<UUID> afterId, boolean sub) implements NoteEdit {
    }

    /** Ticks ({@code done}) or unticks the task. */
    record Tick(UUID listId, UUID taskId, boolean done) implements NoteEdit {
    }

    /** Sets or, with an empty person, clears the assignee. */
    record SetAssignee(UUID listId, UUID taskId, Optional<Person> assignee) implements NoteEdit {
    }

    /** Assigns an open task without assignee to the player who asks. */
    record Claim(UUID listId, UUID taskId) implements NoteEdit {
    }

    /** Switches the scope; Team takes the party the owner is in now. */
    record SetScope(UUID listId, Scope scope) implements NoteEdit {
    }

    record AddGuest(UUID listId, Person guest, Guest.Right right) implements NoteEdit {
    }

    record SetGuestRight(UUID listId, UUID guestId, Guest.Right right) implements NoteEdit {
    }

    record RemoveGuest(UUID listId, UUID guestId) implements NoteEdit {
    }

    /** Adds a goal for {@code itemId} with count 0 and the starting target; the client picks {@code goalId}. */
    record AddGoal(UUID listId, UUID taskId, UUID goalId, String itemId) implements NoteEdit {
    }

    /** Points the goal at another item, keeping count and target. */
    record SetGoalItem(UUID listId, UUID taskId, UUID goalId, String itemId) implements NoteEdit {
    }

    record SetGoalTarget(UUID listId, UUID taskId, UUID goalId, int target) implements NoteEdit {
    }

    /** Sets the shared count; allowed to everyone who may tick, recorded as the goal's last count. */
    record SetGoalCount(UUID listId, UUID taskId, UUID goalId, int count) implements NoteEdit {
    }

    record RemoveGoal(UUID listId, UUID taskId, UUID goalId) implements NoteEdit {
    }

    /** Sets or, with an empty spot, clears the task's spot. */
    record SetSpot(UUID listId, UUID taskId, Optional<Spot> spot) implements NoteEdit {
    }
}
