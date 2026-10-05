package io.github.dirnei.mc.servus.lib.datagen;

import io.github.dirnei.mc.servus.lib.ServusLibIds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusLibIds.MOD_ID)
public final class ServusLibDataGen {

    private ServusLibDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ServusLibLanguageProvider(output));
        generator.addProvider(event.includeClient(), new ServusLibItemModelProvider(output, event.getExistingFileHelper()));
        generator.addProvider(event.includeServer(), new ServusLibRecipeProvider(output, event.getLookupProvider()));
    }
}
