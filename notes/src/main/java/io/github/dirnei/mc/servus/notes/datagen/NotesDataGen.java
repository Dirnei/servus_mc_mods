package io.github.dirnei.mc.servus.notes.datagen;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Registers the Notes data providers; one client data run generates assets and data together. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class NotesDataGen {

    private NotesDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        event.addProvider(new NotesLanguageProvider(event.getGenerator().getPackOutput()));
    }
}
