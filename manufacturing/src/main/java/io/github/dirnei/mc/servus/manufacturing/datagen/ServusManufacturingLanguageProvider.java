package io.github.dirnei.mc.servus.manufacturing.datagen;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.sight.ElementalSight;
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
        add(ServusManufacturingBlocks.ATHANOR.get(), "Athanor");
        add(ServusManufacturingBlocks.GNOME_HOLLOW.get(), "Gnome Hollow");
        add("gui.servus_manufacturing.gnome_hollow.empty", "No vein");
        add("gui.servus_manufacturing.gnome_hollow.ripeness", "Ripeness %s / %s");
        add("gui.servus_manufacturing.gnome_hollow.gnome", "A gnome is at work");
        add("message.servus_manufacturing.gnome_hollow.bound", "A gnome takes your offering and sets to work.");
        add("message.servus_manufacturing.gnome_hollow.no_vein", "Nothing grows here yet. Seed the hollow with a sample of ore.");
        add("message.servus_manufacturing.gnome_hollow.not_ripe", "The vein is not ripe yet.");
        add("message.servus_manufacturing.gnome_hollow.already_bound", "A gnome already works this vein.");
        add("message.servus_manufacturing.gnome_hollow.no_sight", "You cannot see the spirits of the earth.");
        add(ElementalSight.ELIXIR.get(), "Elixir of Sight");
        add(ElementalSight.EFFECT.get(), "Elemental Sight");
        add("gui.servus_manufacturing.athanor.stage.nigredo", "Nigredo");
        add("gui.servus_manufacturing.athanor.stage.albedo", "Albedo");
        add("gui.servus_manufacturing.athanor.stage.citrinitas", "Citrinitas");
        add("gui.servus_manufacturing.athanor.stage.rubedo", "Rubedo");
        add("gui.servus_manufacturing.jei.athanor", "Athanor");
        add("gui.servus_manufacturing.jei.athanor.tier", "Stage: %s");
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
