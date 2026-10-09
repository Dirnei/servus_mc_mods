package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlocks;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusManufacturingLanguageProvider extends LanguageProvider {

    private final LanguageProviderCache book;

    ServusManufacturingLanguageProvider(PackOutput output, LanguageProviderCache book) {
        super(output, ServusManufacturingIds.MOD_ID, "en_us");
        this.book = book;
    }

    @Override
    protected void addTranslations() {
        // Texts of the journal, collected by the Modonomicon book provider.
        book.data().forEach(this::add);
        add(ServusManufacturingBlocks.CALCINER.get(), "Calciner");
        add(ServusManufacturingBlocks.COAGULATOR.get(), "Coagulator");
        add("gui.servus_manufacturing.coagulator.needs", "%s mB %s");
        add("gui.servus_manufacturing.coagulator.chapter", "Needs the journal chapter %s");
        add("journal.servus_manufacturing.villach", "Villach");
        add("journal.servus_manufacturing.schwaz", "Schwaz");
        add("journal.servus_manufacturing.villach.finished", "Finished the Villach chapter of the journal");
        add("journal.servus_manufacturing.schwaz.finished", "Finished the Schwaz chapter of the journal");
        add("advancement.servus_manufacturing.journal.root", "Paracelsus' Journal");
        add("advancement.servus_manufacturing.journal.root.description", "Experience is the master. Chapters of the journal, finished by doing alchemy");
        add("commands.servus_manufacturing.journal.header", "Journal of your team:");
        add("commands.servus_manufacturing.journal.chapter", "%s: %s");
        add("commands.servus_manufacturing.journal.finished", "finished");
        add("commands.servus_manufacturing.journal.open", "open");
        add("commands.servus_manufacturing.journal.task", "  %s: %s / %s");
        add("gui.servus_manufacturing.jei.calcination", "Calcination");
        add("gui.servus_manufacturing.jei.coagulation", "Coagulation");
        add(ServusManufacturingItems.SALT_CRYSTAL.get(), "Salt Crystal");
        add(ServusManufacturingBlocks.SALT_CRYSTAL_BLOCK.get(), "Salt Crystal Block");
        add(ServusManufacturingItems.SULFUR.get(), "Sulfur");
        add(ServusManufacturingItems.QUICKSILVER.get(), "Quicksilver");
        for (Principle principle : Principle.values()) {
            String name = switch (principle) {
                case SALT -> "Salt";
                case SULFUR -> "Sulfur";
                case MERCURY -> "Mercury";
            };
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            add("fluid_type." + ServusManufacturingIds.MOD_ID + "." + principle.id(), name);
            add(entry.block().get(), name);
            add(entry.bucket().get(), name + " Bucket");
        }
    }
}
