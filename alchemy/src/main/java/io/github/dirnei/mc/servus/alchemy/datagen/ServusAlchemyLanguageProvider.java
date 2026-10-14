package io.github.dirnei.mc.servus.alchemy.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.PrincipleText;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import io.github.dirnei.mc.servus.alchemy.sight.ElementalSight;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

final class ServusAlchemyLanguageProvider extends LanguageProvider {

    private final LanguageProviderCache book;

    ServusAlchemyLanguageProvider(PackOutput output, LanguageProviderCache book) {
        super(output, ServusAlchemyIds.MOD_ID, "en_us");
        this.book = book;
    }

    @Override
    protected void addTranslations() {
        book.data().forEach(this::add);
        add(ServusAlchemyBlocks.CALCINER.get(), "Calciner");
        add(ServusAlchemyBlocks.FIREBOX.get(), "Firebox");
        add(ServusAlchemyBlocks.ASH_PAN.get(), "Ash Pan");
        add(ServusAlchemyBlocks.ALEMBIC.get(), "Alembic");
        add(ServusAlchemyBlocks.RECEIVER.get(), "Receiver");
        add(ServusAlchemyBlocks.SULFUR_HOOD.get(), "Sulfur Hood");
        add(ServusAlchemyBlocks.COPPER_DUCT.get(), "Copper Duct");
        add(ServusAlchemyBlocks.PURIFIER.get(), "Purifier");
        add(ServusAlchemyBlocks.CATCH_BASIN.get(), "Catch Basin");
        add(ServusAlchemyBlocks.RESIDUE_CROCK.get(), "Residue Crock");
        add("message.servus_manufacturing.purifier.formed", "The Purifier is ready.");
        add("message.servus_manufacturing.purifier.missing",
                "The Purifier is not complete: %s needed %s. The Purifier page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.purifier.at.below_the_vat", "below the Vat");
        add("message.servus_manufacturing.purifier.at.beside_the_log", "beside the log");
        add("message.servus_manufacturing.purifier.at.on_the_other_side_of_the_log", "on the other side of the log");
        add("message.servus_manufacturing.purifier.at.itself", "here");
        add("jade.servus_manufacturing.purifier.residue", "Caput Mortuum: %s");
        add(ServusAlchemyBlocks.VAT.get(), "Principle Vat");
        add("message.servus_manufacturing.coagulator.formed", "The Coagulator is ready.");
        add("message.servus_manufacturing.athanor.formed", "The Athanor is ready.");
        add(ServusAlchemyBlocks.ATHANOR_CROWN.get(), "Athanor");
        add("message.servus_manufacturing.athanor.missing_tank",
                "The Athanor is not complete: a Fluid Tank is needed %s, two blocks out. The Athanor page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.athanor.missing_duct",
                "The Athanor is not complete: two Copper Ducts are needed one block up toward the tank %s. The Athanor page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.athanor.tank.left", "left of it");
        add("message.servus_manufacturing.athanor.tank.back", "behind it");
        add("message.servus_manufacturing.athanor.tank.right", "right of it");
        add("message.servus_manufacturing.athanor.missing",
                "The Athanor needs a hearth: a diamond of 13 Bricks right below it. The Athanor page of Paracelsus' Journal shows it.");
        add("message.servus_manufacturing.coagulator.missing",
                "The Coagulator is not complete: %s needed %s. The Coagulator page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.coagulator.at.left_of_the_coagulator", "left of the Coagulator");
        add("message.servus_manufacturing.coagulator.at.right_of_the_coagulator", "right of the Coagulator");
        add("message.servus_manufacturing.coagulator.at.above_the_coagulator", "above the Coagulator");
        add("message.servus_manufacturing.coagulator.at.left_of_the_middle_vat", "left of the middle Vat");
        add("message.servus_manufacturing.coagulator.at.right_of_the_middle_vat", "right of the middle Vat");
        add("message.servus_manufacturing.coagulator.at.itself", "here");
        add("jade.servus_manufacturing.purifier.items", "Ash: %s");
        add("jade.servus_manufacturing.purifier.water", "%s / %s mB Water");
        add(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(), "Calciner Stone Bricks");
        add(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), "Calciner Deepslate Tiles");
        add("message.servus_manufacturing.calciner.formed", "The Calciner is ready.");
        add("jade.servus_manufacturing.calciner.input", "Matter: %s");
        add("jade.servus_manufacturing.calciner.fuel", "Fuel: %s");
        add("jade.servus_manufacturing.calciner.progress", "Progress: %s%%");
        add("jade.servus_manufacturing.calciner.principle", "%s / %s mB %s");
        add("jade.servus_manufacturing.calciner.ash", "Ash: %s / %s");
        add("jade.servus_manufacturing.calciner.stack", "%s %s");
        add("jade.servus_manufacturing.calciner.empty", "none");
        add("config.jade.plugin_servus_manufacturing.calciner", "Calciner");
        add("config.jade.plugin_servus_manufacturing.purifier", "Purifier");
        add("config.jade.plugin_servus_manufacturing.athanor", "Athanor");
        add("jade.servus_manufacturing.athanor.stage", "Stage: %s");
        add("jade.servus_manufacturing.athanor.unformed", "Not formed (stage %s)");
        add("jade.servus_manufacturing.athanor.input", "Burning: %s");
        add("jade.servus_manufacturing.athanor.output", "Made: %s");
        add("jade.servus_manufacturing.athanor.principle", "%s mB %s in the tanks");
        add("message.servus_manufacturing.calciner.missing",
                "The Calciner is not complete: %s needed %s. The Calciner page of Paracelsus' Journal shows the build.");
        add("message.servus_manufacturing.calciner.left_of", "left of the %s");
        add("message.servus_manufacturing.calciner.right_of", "right of the %s");
        add("message.servus_manufacturing.calciner.above", "above the %s");
        add("message.servus_manufacturing.calciner.below", "below the %s");
        add(ServusAlchemyBlocks.COAGULATOR.get(), "Coagulator");
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
        add(ServusAlchemyItems.ASH.get(), "Ash");
        add(ServusAlchemyItems.CAPUT_MORTUUM.get(), "Caput Mortuum");
        add(ServusAlchemyItems.SALT_CRYSTAL.get(), "Salt Crystal");
        add("gui.servus_manufacturing.jei.purification", "Purification");
        add(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(), "Salt Crystal Block");
        add(ServusAlchemyBlocks.ATHANOR.get(), "Athanor");
        add(ServusAlchemyBlocks.GNOME_HOLLOW.get(), "Gnome Hollow");
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
        add(ServusAlchemyItems.SULFUR.get(), "Sulfur");
        add(ServusAlchemyItems.QUICKSILVER.get(), "Quicksilver");
        for (Principle principle : Principle.values()) {
            String name = PrincipleText.name(principle);
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            add("fluid_type." + ServusAlchemyIds.MOD_ID + "." + principle.id(), name);
            add(entry.block().get(), name);
            add(entry.bucket().get(), name + " Bucket");
        }
    }
}
