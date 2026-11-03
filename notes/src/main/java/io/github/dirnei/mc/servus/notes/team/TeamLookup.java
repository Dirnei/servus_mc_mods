package io.github.dirnei.mc.servus.notes.team;

import java.util.Optional;
import java.util.UUID;

/** Answers which party a player is in. Server thread only. */
public interface TeamLookup {

    /** Whether a team mod runs, so the Team scope exists. */
    boolean installed();

    /** The party {@code player} is in, empty when in none. */
    Optional<UUID> party(UUID player);
}
