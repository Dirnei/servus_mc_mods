package io.github.dirnei.mc.servus.notes.team;

import java.util.Optional;
import java.util.UUID;

/** No team mod: nobody is in a party and the Team scope does not exist. */
public final class NoTeams implements TeamLookup {

    public static final NoTeams INSTANCE = new NoTeams();

    private NoTeams() {
    }

    @Override
    public boolean installed() {
        return false;
    }

    @Override
    public Optional<UUID> party(UUID player) {
        return Optional.empty();
    }
}
