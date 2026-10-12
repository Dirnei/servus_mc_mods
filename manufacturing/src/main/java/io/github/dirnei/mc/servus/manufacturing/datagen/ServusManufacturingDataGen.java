package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.datagen.book.JournalResearch;
import com.klikli_dev.modonomicon.api.datagen.research.ResearchCache;
import com.klikli_dev.modonomicon.api.datagen.NeoResearchProvider;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataExportProvider;
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
import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import io.github.dirnei.mc.servus.manufacturing.datagen.book.JournalBook;
import net.minecraft.data.advancements.AdvancementProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingDataGen {

    private ServusManufacturingDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        // The book and research providers fill the language cache, so they run before the language provider writes it.
        LanguageProviderCache bookLang = new LanguageProviderCache("en_us");
        ResearchCache research = new ResearchCache();
        event.addProvider(NeoBookProvider.of(event, bookLang, research, new JournalBook()));
        event.addProvider(NeoResearchProvider.of(event, bookLang, research, new JournalResearch()));
        event.addProvider(new ServusManufacturingLanguageProvider(output, bookLang));
        event.addProvider(new ServusManufacturingBlockStateProvider(output));
        event.addProvider(new ServusManufacturingItemModelProvider(output));

        event.addProvider(new ServusManufacturingBlockTagsProvider(output, lookup));
        event.addProvider(new ServusManufacturingRecipeProvider.Runner(output, lookup));
        event.addProvider(new ServusManufacturingMultiblockProvider(output));
        event.addProvider(new AdvancementProvider(output, lookup, List.of(new JournalAdvancements())));
        event.addProvider(new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusManufacturingBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        // Last: reads the recipes and lang file written above.
        event.addProvider(new GameDataExportProvider());
    }
}
