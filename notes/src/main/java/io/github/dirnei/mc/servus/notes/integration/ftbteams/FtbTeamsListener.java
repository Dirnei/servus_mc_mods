package io.github.dirnei.mc.servus.notes.integration.ftbteams;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.neoforge.FTBTeamsEvent;
import io.github.dirnei.mc.servus.notes.ServusNotes;
import io.github.dirnei.mc.servus.notes.server.NotesService;
import io.github.dirnei.mc.servus.notes.team.NoTeams;
import io.github.dirnei.mc.servus.notes.team.Teams;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Hooks Notes into FTB Teams: installs the party lookup and follows joins, leaves and deleted
 * parties. The only entry point into this package; call it only when FTB Teams is loaded.
 */
public final class FtbTeamsListener {

    private FtbTeamsListener() {
    }

    /** Installs the lookup and the listeners; a broken FTB Teams leaves Notes without teams and logs why. */
    public static void install() {
        try {
            Teams.install(new FtbTeamsLookup());
            NeoForge.EVENT_BUS.addListener(FTBTeamsEvent.PlayerJoinedPartyTeam.class,
                    event -> NotesService.refreshLater(event.getEventData().player().getUUID()));
            NeoForge.EVENT_BUS.addListener(FTBTeamsEvent.PlayerLeftPartyTeam.class,
                    event -> NotesService.refreshLater(event.getEventData().playerId()));
            NeoForge.EVENT_BUS.addListener(FTBTeamsEvent.TeamDeleted.class, FtbTeamsListener::teamDeleted);
            ServusNotes.LOGGER.info("Servus: Notes uses FTB Teams parties for Team lists");
        } catch (LinkageError e) {
            Teams.install(NoTeams.INSTANCE);
            ServusNotes.LOGGER.error("FTB Teams does not match what Notes expects; Team lists are off", e);
        }
    }

    private static void teamDeleted(FTBTeamsEvent.TeamDeleted event) {
        if (FTBTeamsAPI.api().isManagerLoaded()) {
            NotesService.teamDeleted(FTBTeamsAPI.api().getManager().getServer(), event.getEventData().team().getId());
        }
    }
}
