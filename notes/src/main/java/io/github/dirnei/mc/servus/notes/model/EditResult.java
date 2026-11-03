package io.github.dirnei.mc.servus.notes.model;

import java.util.Set;
import java.util.UUID;

/** What {@link NoteEdits#apply} made of an edit. */
public sealed interface EditResult {

    /**
     * The edit was accepted. {@code changed} holds the ids of the lists that differ from before,
     * deleted ones included; it is empty when the edit changed nothing.
     */
    record Applied(NotesBook book, Set<UUID> changed) implements EditResult {

        public Applied {
            changed = Set.copyOf(changed);
        }
    }

    record Refused(Refusal reason) implements EditResult {
    }
}
