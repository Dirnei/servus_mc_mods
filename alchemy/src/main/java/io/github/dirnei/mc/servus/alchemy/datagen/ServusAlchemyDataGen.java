package io.github.dirnei.mc.servus.alchemy.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import com.klikli_dev.modonomicon.api.datagen.NeoResearchProvider;
import com.klikli_dev.modonomicon.api.datagen.research.ResearchCache;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.datagen.book.JournalBook;
import io.github.dirnei.mc.servus.alchemy.datagen.book.JournalResearch;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusAlchemyIds.MOD_ID)
public final class ServusAlchemyDataGen {

    private ServusAlchemyDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        // The book and research providers fill the language cache, so they must run before the language provider.
        LanguageProviderCache bookLang = new LanguageProviderCache("en_us");
        ResearchCache research = new ResearchCache();
        event.addProvider(NeoBookProvider.of(event, bookLang, research, new JournalBook()));
        event.addProvider(NeoResearchProvider.of(event, bookLang, research, new JournalResearch()));
        event.addProvider(new ServusAlchemyLanguageProvider(output, bookLang));
        event.addProvider(new ServusAlchemyBlockStateProvider(output));
        event.addProvider(new ServusAlchemyItemModelProvider(output));
        event.addProvider(new ServusAlchemySoundsProvider(output));

        event.addProvider(new ServusAlchemyBlockTagsProvider(output, lookup));
        event.addProvider(new ServusAlchemyItemTagsProvider(output, lookup));
        event.addProvider(new ServusAlchemyBiomeTagsProvider(output, lookup));
        event.addProvider(new ServusAlchemyRecipeProvider.Runner(output, lookup));
        event.addProvider(new ServusAlchemyMultiblockProvider(output));
        event.addProvider(new AdvancementProvider(output, lookup, List.of(new JournalAdvancements())));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusAlchemyBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
    }
}
