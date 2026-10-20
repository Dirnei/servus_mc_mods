package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import net.minecraft.data.PackOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Registers the Brewery data providers; one client data run generates assets and data together. */
@EventBusSubscriber(modid = ServusBreweryIds.MOD_ID, value = Dist.CLIENT)
public final class ServusBreweryDataGen {

    private ServusBreweryDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.addProvider(new ServusBreweryLanguageProvider(output));
        event.addProvider(new ServusBreweryItemModelProvider(output));
    }
}
