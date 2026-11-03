package io.github.dirnei.mc.servus.notes.model;

import java.util.Optional;
import java.util.UUID;

/** Who may do what with a list. {@link NoteEdits} asks only this, so the rules can change without touching the edits. */
public interface Access {

    boolean canSee(UUID actor, NoteList list);

    /** Add, change, move and delete tasks, rename the list, set assignees. */
    boolean canEdit(UUID actor, NoteList list);

    /** Tick, untick and claim tasks. */
    boolean canTick(UUID actor, NoteList list);

    /** Share the list, change its scope, delete it. */
    boolean canManage(UUID actor, NoteList list);

    /** The FTB Teams party the actor is in, which a list switched to Team keeps; empty without party or FTB Teams. */
    default Optional<UUID> party(UUID actor) {
        return Optional.empty();
    }

    /** All four answers at once, for the snapshot a player receives. */
    default Rights rights(UUID actor, NoteList list) {
        return new Rights(canSee(actor, list), canEdit(actor, list), canTick(actor, list), canManage(actor, list));
    }
}
