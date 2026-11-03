package io.github.dirnei.mc.servus.notes;

import io.github.dirnei.mc.servus.notes.config.NotesClientConfig;
import io.github.dirnei.mc.servus.notes.config.NotesServerConfig;
import io.github.dirnei.mc.servus.notes.gametest.NotesGameTests;
import io.github.dirnei.mc.servus.notes.integration.ftbteams.FtbTeamsListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Mod entrypoint for Servus: Notes. Notes depends on no other Servus mod. */
@Mod(ServusNotesIds.MOD_ID)
public final class ServusNotes {

    public static final Logger LOGGER = LoggerFactory.getLogger(ServusNotes.class);

    private static final String FTB_TEAMS = "ftbteams";

    public ServusNotes(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, NotesServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, NotesClientConfig.SPEC);
        NotesGameTests.register(modEventBus);
        if (ModList.get().isLoaded(FTB_TEAMS)) {
            FtbTeamsListener.install();
        }
        LOGGER.info("Servus: Notes loaded");
    }
}
