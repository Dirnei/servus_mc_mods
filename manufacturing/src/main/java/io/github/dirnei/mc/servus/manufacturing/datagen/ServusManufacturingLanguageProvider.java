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
        add(ServusManufacturingBlocks.FIREBOX.get(), "Firebox");
        add(ServusManufacturingBlocks.ASH_PAN.get(), "Ash Pan");
        add(ServusManufacturingBlocks.ALEMBIC.get(), "Alembic");
        add(ServusManufacturingBlocks.RECEIVER.get(), "Receiver");
        add(ServusManufacturingBlocks.SULFUR_HOOD.get(), "Sulfur Hood");
        add(ServusManufacturingBlocks.SULFUR_DUCT.get(), "Sulfur Duct");
        add(ServusManufacturingBlocks.CALCINER_SHELL.get(), "Calciner Bricks");
        add("message.servus_manufacturing.calciner.formed", "The Calciner is ready.");
        add("jade.servus_manufacturing.calciner.input", "Matter: %s");
        add("jade.servus_manufacturing.calciner.fuel", "Fuel: %s");
        add("jade.servus_manufacturing.calciner.progress", "Progress: %s%%");
        add("jade.servus_manufacturing.calciner.principle", "%s / %s mB %s");
        add("jade.servus_manufacturing.calciner.stack", "%s %s");
        add("jade.servus_manufacturing.calciner.empty", "none");
        add("config.jade.plugin_servus_manufacturing.calciner", "Calciner");
        add("message.servus_manufacturing.calciner.missing",
                "The Calciner is not complete: %s needed %s. The Calciner page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.calciner.left_of", "left of the %s");
        add("message.servus_manufacturing.calciner.right_of", "right of the %s");
        add("message.servus_manufacturing.calciner.above", "above the %s");
        add("message.servus_manufacturing.calciner.below", "below the %s");
        add(ServusManufacturingBlocks.COAGULATOR.get(), "Coagulator");
        add("gui.servus_manufacturing.coagulator.needs", "%s mB %s");
        add("gui.servus_manufacturing.coagulator.select", "Select recipe");
        add("gui.servus_manufacturing.coagulator.pick", "Pick a recipe");
        add("gui.servus_manufacturing.coagulator.chapter", "Needs the journal chapter %s");
        add("journal.servus_manufacturing.villach", "Villach");
        add("journal.servus_manufacturing.schwaz", "Schwaz");
        add("journal.servus_manufacturing.villach.finished", "Finished the Villach chapter of the journal");
        add("journal.servus_manufacturing.schwaz.finished", "Finished the Schwaz chapter of the journal");
        add("item.servus_manufacturing.paracelsus_journal", "Paracelsus' Journal");
        add("advancement.servus_manufacturing.journal.root", "Paracelsus' Journal");
        add("advancement.servus_manufacturing.journal.root.description", "Experience is the master. Chapters of the journal, finished by doing alchemy");
        add("commands.servus_manufacturing.journal.header", "Journal of your team:");
        add("commands.servus_manufacturing.journal.chapter", "%s: %s");
        add("commands.servus_manufacturing.journal.finished", "finished");
        add("commands.servus_manufacturing.journal.open", "open");
        add("commands.servus_manufacturing.journal.task", "  %s: %s / %s");
        add("gui.servus_manufacturing.jei.calcination", "Calcination");
        add("gui.servus_manufacturing.jei.coagulation", "Coagulation");
        add(ServusManufacturingItems.ASH.get(), "Ash");
        add(ServusManufacturingItems.SALT_CRYSTAL.get(), "Salt Crystal");
        add("gui.servus_manufacturing.jei.purification", "Purification");
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
