package io.github.dirnei.mc.servus.notes.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The sharing rules: the owner does everything; guests see, edit guests edit; members of a Team
 * list's party see and edit while FTB Teams runs; everyone sees Server lists and ops edit them.
 * Whoever sees a list may tick and claim; only the owner shares, switches the scope and deletes.
 * Each player is looked up once per instance, so make one per edit or broadcast.
 */
public final class SharedAccess implements Access {

    private final Function<UUID, Viewer> lookup;
    private final Map<UUID, Viewer> viewers = new HashMap<>();

    /** Rules that ask {@code lookup} who a player is. */
    public SharedAccess(Function<UUID, Viewer> lookup) {
        this.lookup = lookup;
    }

    private Viewer viewer(UUID actor) {
        return viewers.computeIfAbsent(actor, lookup);
    }

    private static boolean owns(UUID actor, NoteList list) {
        return list.owner().equals(actor);
    }

    private boolean inTeam(UUID actor, NoteList list) {
        Viewer viewer = viewer(actor);
        return list.scope() == Scope.TEAM && viewer.teamsInstalled()
                && viewer.party().isPresent() && viewer.party().equals(list.team());
    }

    @Override
    public boolean canSee(UUID actor, NoteList list) {
        return owns(actor, list) || list.guest(actor).isPresent() || list.scope() == Scope.SERVER || inTeam(actor, list);
    }

    @Override
    public boolean canEdit(UUID actor, NoteList list) {
        return owns(actor, list)
                || list.guest(actor).map(guest -> guest.right() == Guest.Right.EDIT).orElse(false)
                || inTeam(actor, list)
                || (list.scope() == Scope.SERVER && viewer(actor).op());
    }

    @Override
    public boolean canTick(UUID actor, NoteList list) {
        return canSee(actor, list);
    }

    @Override
    public boolean canManage(UUID actor, NoteList list) {
        return owns(actor, list);
    }

    @Override
    public Optional<UUID> party(UUID actor) {
        Viewer viewer = viewer(actor);
        return viewer.teamsInstalled() ? viewer.party() : Optional.empty();
    }
}
