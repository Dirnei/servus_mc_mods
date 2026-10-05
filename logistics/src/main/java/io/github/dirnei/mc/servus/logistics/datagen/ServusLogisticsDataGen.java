package io.github.dirnei.mc.servus.logistics.datagen;

import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusLogisticsIds.MOD_ID)
public final class ServusLogisticsDataGen {

    private ServusLogisticsDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ServusLogisticsLanguageProvider(output));
        generator.addProvider(event.includeClient(), new ServusLogisticsBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ServusLogisticsItemModelProvider(output, existingFileHelper));

        generator.addProvider(event.includeServer(), new ServusLogisticsBlockTagsProvider(output, lookup, existingFileHelper));
        generator.addProvider(event.includeServer(), new ServusLogisticsRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusLogisticsBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
