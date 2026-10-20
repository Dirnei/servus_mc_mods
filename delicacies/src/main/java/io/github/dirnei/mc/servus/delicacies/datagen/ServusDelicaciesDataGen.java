package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
import net.minecraft.data.PackOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** Registers the Delicacies data providers; one client data run generates assets and data together. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID, value = Dist.CLIENT)
public final class ServusDelicaciesDataGen {

    private ServusDelicaciesDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        event.addProvider(new ServusDelicaciesLanguageProvider(output));
        event.addProvider(new ServusDelicaciesItemModelProvider(output));
    }
}
