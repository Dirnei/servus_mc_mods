package io.github.dirnei.mc.servus.delicacies.datagen;

import io.github.dirnei.mc.servus.delicacies.ServusDelicaciesIds;
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

/** Registers the Delicacies data providers; one client data run generates assets and data together. */
@EventBusSubscriber(modid = ServusDelicaciesIds.MOD_ID, value = Dist.CLIENT)
public final class ServusDelicaciesDataGen {

    private ServusDelicaciesDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        PackOutput output = event.getGenerator().getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        event.addProvider(new ServusDelicaciesLanguageProvider(output));
        event.addProvider(new ServusDelicaciesBlockStateProvider(output));
        event.addProvider(new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ServusDelicaciesBlockLoot::new, LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ServusDelicaciesSeedLoot.grass(), LootContextParamSets.BLOCK),
                new LootTableProvider.SubProviderEntry(ServusDelicaciesSeedLoot.village(), LootContextParamSets.CHEST)),
                lookup));
        event.addProvider(new ServusDelicaciesLootModifierProvider(output, lookup));
        event.addProvider(new ServusDelicaciesBlockTagsProvider(output, lookup));
        event.addProvider(new ServusDelicaciesItemTagsProvider(output, lookup));
        event.addProvider(new ServusDelicaciesDataMapProvider(output, lookup));
        event.addProvider(new ServusDelicaciesRecipeProvider.Runner(output, lookup));
    }
}
