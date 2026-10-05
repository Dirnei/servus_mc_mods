package io.github.dirnei.mc.servus.storage.datagen;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
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

@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStorageDataGen {

    private ServusStorageDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new ServusStorageLanguageProvider(output));
        generator.addProvider(event.includeClient(), new ServusStorageBlockStateProvider(output, existingFileHelper));

        generator.addProvider(event.includeServer(), new ServusStorageBlockTagsProvider(output, lookup, existingFileHelper));
        generator.addProvider(event.includeServer(), new ServusStorageRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusStorageBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
