package io.github.dirnei.mc.servus.notes.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A named list of tasks in their order, owned by one player, with its scope and guests.
 *
 * @param team the FTB Teams party of a Team list; always empty for other scopes, and a Team list without one reads as Private
 */
public record NoteList(UUID id, String name, UUID owner, String ownerName, List<Task> tasks,
                       Scope scope, Optional<UUID> team, List<Guest> guests) {

    public NoteList {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(ownerName, "ownerName");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(team, "team");
        tasks = List.copyOf(tasks);
        guests = List.copyOf(guests);
        if (scope != Scope.TEAM) {
            team = Optional.empty();
        } else if (team.isEmpty()) {
            scope = Scope.PRIVATE;
        }
    }

    /** A private list without guests. */
    public NoteList(UUID id, String name, UUID owner, String ownerName, List<Task> tasks) {
        this(id, name, owner, ownerName, tasks, Scope.PRIVATE, Optional.empty(), List.of());
    }

    /** A new empty private list owned by {@code owner}. */
    public static NoteList create(UUID id, String name, Person owner) {
        return new NoteList(id, name, owner.id(), owner.name(), List.of());
    }

    public Person ownerPerson() {
        return new Person(owner, ownerName);
    }

    /** How many tasks are not ticked, subtasks included. */
    public int openCount() {
        return (int) tasks.stream().filter(task -> !task.isDone()).count();
    }

    public Optional<Task> task(UUID taskId) {
        return tasks.stream().filter(task -> task.id().equals(taskId)).findFirst();
    }

    /** The position of the task, or -1 when the list has no such task. */
    public int indexOf(UUID taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(taskId)) {
                return i;
            }
        }
        return -1;
    }

    /** The guest entry of {@code player}, if they are a guest. */
    public Optional<Guest> guest(UUID player) {
        return guests.stream().filter(guest -> guest.who().id().equals(player)).findFirst();
    }

    public NoteList withName(String newName) {
        return new NoteList(id, newName, owner, ownerName, tasks, scope, team, guests);
    }

    public NoteList withTasks(List<Task> newTasks) {
        return new NoteList(id, name, owner, ownerName, newTasks, scope, team, guests);
    }

    /** This list with a new scope; {@code newTeam} counts only for {@link Scope#TEAM}. */
    public NoteList withScope(Scope newScope, Optional<UUID> newTeam) {
        return new NoteList(id, name, owner, ownerName, tasks, newScope, newTeam, guests);
    }

    public NoteList withGuests(List<Guest> newGuests) {
        return new NoteList(id, name, owner, ownerName, tasks, scope, team, newGuests);
    }

    /** This list owned by {@code newOwner}, who is no longer a guest of it. */
    public NoteList withOwner(Person newOwner) {
        List<Guest> others = guests.stream().filter(guest -> !guest.who().id().equals(newOwner.id())).toList();
        return new NoteList(id, name, newOwner.id(), newOwner.name(), tasks, scope, team, others);
    }
}
