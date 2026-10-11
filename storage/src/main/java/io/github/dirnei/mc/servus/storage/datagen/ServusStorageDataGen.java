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
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusStorageIds.MOD_ID)
public final class ServusStorageDataGen {

    private ServusStorageDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ServusStorageLanguageProvider(output));
        event.addProvider(new ServusStorageBlockStateProvider(output));
        event.addProvider(new ServusStorageBlockTagsProvider(output, lookup));
        event.addProvider(new ServusStorageItemTagsProvider(output, lookup));
        event.addProvider(new ServusStorageRecipeProvider.Runner(output, lookup));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusStorageBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
