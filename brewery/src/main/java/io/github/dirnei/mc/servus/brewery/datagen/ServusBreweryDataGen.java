package io.github.dirnei.mc.servus.brewery.datagen;

import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
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
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ServusBreweryLanguageProvider(output));
        event.addProvider(new ServusBreweryItemModelProvider(output));
        event.addProvider(new ServusBreweryBlockStateProvider(output));
        event.addProvider(new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ServusBreweryBlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ServusBreweryExtraLoot.Grass::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ServusBreweryExtraLoot.Chests::new, LootContextParamSets.CHEST),
                new LootTableProvider.SubProviderEntry(ServusBreweryExtraLoot.Harvest::new, LootContextParamSets.BLOCK_INTERACT)),
                lookup));
        event.addProvider(new ServusBreweryLootModifierProvider(output, lookup));
        event.addProvider(new ServusBreweryBlockTagsProvider(output, lookup));
        event.addProvider(new ServusBreweryItemTagsProvider(output, lookup));
        event.addProvider(new ServusBreweryDataMapProvider(output, lookup));
        event.addProvider(new ServusBreweryRecipeProvider.Runner(output, lookup));
    }
}
