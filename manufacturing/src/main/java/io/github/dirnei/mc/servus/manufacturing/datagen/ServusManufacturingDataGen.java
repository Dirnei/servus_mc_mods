package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ServusManufacturingDataGen {

    private ServusManufacturingDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ServusManufacturingLanguageProvider(output));
        generator.addProvider(event.includeClient(), new ServusManufacturingBlockStateProvider(output, event.getExistingFileHelper()));
    }
}
