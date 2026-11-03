package io.github.dirnei.mc.servus.notes.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Every list on the server by id, in the order they were made. Immutable; edits return a new book. */
public final class NotesBook {

    public static final NotesBook EMPTY = new NotesBook(List.of());

    private final Map<UUID, NoteList> lists;

    public NotesBook(Collection<NoteList> lists) {
        Map<UUID, NoteList> byId = new LinkedHashMap<>();
        for (NoteList list : lists) {
            byId.put(list.id(), list);
        }
        this.lists = Collections.unmodifiableMap(byId);
    }

    public Optional<NoteList> get(UUID listId) {
        return Optional.ofNullable(lists.get(listId));
    }

    public Collection<NoteList> all() {
        return lists.values();
    }

    /** The lists {@code player} owns, in the order they were made. */
    public List<NoteList> ownedBy(UUID player) {
        return lists.values().stream().filter(list -> list.owner().equals(player)).toList();
    }

    /** This book with {@code list} added, or replacing the list with its id in place. */
    public NotesBook with(NoteList list) {
        Map<UUID, NoteList> copy = new LinkedHashMap<>(lists);
        copy.put(list.id(), list);
        return new NotesBook(copy.values());
    }

    public NotesBook without(UUID listId) {
        Map<UUID, NoteList> copy = new LinkedHashMap<>(lists);
        copy.remove(listId);
        return new NotesBook(copy.values());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NotesBook book && List.copyOf(lists.values()).equals(List.copyOf(book.lists.values()));
    }

    @Override
    public int hashCode() {
        return List.copyOf(lists.values()).hashCode();
    }

    @Override
    public String toString() {
        return "NotesBook" + lists.values();
    }
}
