package io.github.dirnei.mc.servus.notes.integration.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import io.github.dirnei.mc.servus.notes.ServusNotes;
import io.github.dirnei.mc.servus.notes.team.TeamLookup;
import java.util.Optional;
import java.util.UUID;

/** Parties from FTB Teams. Only party teams count; a player's own solo team is no party. */
final class FtbTeamsLookup implements TeamLookup {

    private boolean failed;

    @Override
    public boolean installed() {
        return true;
    }

    @Override
    public Optional<UUID> party(UUID player) {
        if (failed) {
            return Optional.empty();
        }
        try {
            if (!FTBTeamsAPI.api().isManagerLoaded()) {
                return Optional.empty();
            }
            return FTBTeamsAPI.api().getManager().getTeamForPlayerID(player).filter(Team::isPartyTeam).map(Team::getId);
        } catch (RuntimeException | LinkageError e) {
            failed = true;
            ServusNotes.LOGGER.error("FTB Teams lookup failed; Notes treats every player as in no party from now on", e);
            return Optional.empty();
        }
    }
}
