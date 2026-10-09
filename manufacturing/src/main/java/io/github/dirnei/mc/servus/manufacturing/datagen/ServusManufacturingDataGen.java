package io.github.dirnei.mc.servus.manufacturing.datagen;

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
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ServusManufacturingIds.MOD_ID)
public final class ServusManufacturingDataGen {

    private ServusManufacturingDataGen() {
    }

    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        // The book provider fills the language cache, so it runs before the language provider writes it.
        LanguageProviderCache bookLang = new LanguageProviderCache("en_us");
        generator.addProvider(event.includeServer(), NeoBookProvider.of(event, lookup, new JournalBook(bookLang)));
        generator.addProvider(event.includeClient(), new ServusManufacturingLanguageProvider(output, bookLang));
        generator.addProvider(event.includeClient(), new ServusManufacturingBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(), new ServusManufacturingItemModelProvider(output, existingFileHelper));

        generator.addProvider(event.includeServer(), new ServusManufacturingBlockTagsProvider(output, lookup, existingFileHelper));
        generator.addProvider(event.includeServer(), new ServusManufacturingRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new ServusManufacturingMultiblockProvider(output));
        generator.addProvider(event.includeServer(), new AdvancementProvider(output, lookup, existingFileHelper,
                List.of(new JournalAdvancements())));
        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ServusManufacturingBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        // Last: reads the recipes and lang file written above.
        generator.addProvider(event.includeServer(), new GameDataExportProvider());
    }
}
