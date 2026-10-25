package io.github.dirnei.mc.servus.shuttles.datagen;

import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
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

@EventBusSubscriber(modid = ServusShuttlesIds.MOD_ID)
public final class ServusShuttlesDataGen {

    private ServusShuttlesDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ServusShuttlesLanguageProvider(output));
        event.addProvider(new ServusShuttlesSoundsProvider(output));
        event.addProvider(new ServusShuttlesBlockStateProvider(output));
        event.addProvider(new ServusShuttlesBlockTagsProvider(output, lookup));
        event.addProvider(new ServusShuttlesItemTagsProvider(output, lookup));
        event.addProvider(new ServusShuttlesRecipeProvider.Runner(output, lookup));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusShuttlesBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
