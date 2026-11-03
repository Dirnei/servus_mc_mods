package io.github.dirnei.mc.servus.notes.model;

import java.util.UUID;

/** Every list is private: its owner may do everything, nobody else anything. */
public final class OwnerAccess implements Access {

    public static final OwnerAccess INSTANCE = new OwnerAccess();

    private OwnerAccess() {
    }

    private static boolean owns(UUID actor, NoteList list) {
        return list.owner().equals(actor);
    }

    @Override
    public boolean canSee(UUID actor, NoteList list) {
        return owns(actor, list);
    }

    @Override
    public boolean canEdit(UUID actor, NoteList list) {
        return owns(actor, list);
    }

    @Override
    public boolean canTick(UUID actor, NoteList list) {
        return owns(actor, list);
    }

    @Override
    public boolean canManage(UUID actor, NoteList list) {
        return owns(actor, list);
    }
}
