package io.github.dirnei.mc.servus.foundation.datagen;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.logistics.datagen.ServusLogisticsBlockLoot;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/** One client data run generates assets and data together, as NeoForge recommends since 21.4. */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class ServusFoundationDataGen {

    private ServusFoundationDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ServusFoundationLanguageProvider(output));
        event.addProvider(new ServusFoundationBlockStateProvider(output));
        event.addProvider(new ServusFoundationItemModelProvider(output));
        event.addProvider(new ServusFoundationBlockTagsProvider(output, lookup));
        event.addProvider(new ServusFoundationItemTagsProvider(output, lookup));
        event.addProvider(new ServusFoundationRecipeProvider.Runner(output, lookup));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusLogisticsBlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(AdapterBlockLoot::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(MachineBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
