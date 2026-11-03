package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * What the sharing rules need to know about a player.
 *
 * @param op             whether the player has op rights (permission level 2)
 * @param party          the FTB Teams party the player is in, empty when in none or without FTB Teams
 * @param teamsInstalled whether FTB Teams runs on the server; without it Team lists are seen by owner and guests only
 */
public record Viewer(UUID id, boolean op, Optional<UUID> party, boolean teamsInstalled) {

    public Viewer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(party, "party");
    }

    /** A player without op rights or party on a server without FTB Teams. */
    public static Viewer plain(UUID id) {
        return new Viewer(id, false, Optional.empty(), false);
    }
}
