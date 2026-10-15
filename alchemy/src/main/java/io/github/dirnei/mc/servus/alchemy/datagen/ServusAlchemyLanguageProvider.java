package io.github.dirnei.mc.servus.alchemy.datagen;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import io.github.dirnei.mc.servus.alchemy.Principle;
import io.github.dirnei.mc.servus.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.alchemy.PrincipleText;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyBlocks;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyEntities;
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
        add("message.servus_alchemy.purifier.formed", "The Purifier is ready.");
        add("message.servus_alchemy.purifier.missing",
                "The Purifier is not complete: %s needed %s. The Purifier page of Paracelsus' Journal shows the build.");
        add("message.servus_alchemy.purifier.at.below_the_vat", "below the Vat");
        add("message.servus_alchemy.purifier.at.beside_the_log", "beside the log");
        add("message.servus_alchemy.purifier.at.on_the_other_side_of_the_log", "on the other side of the log");
        add("message.servus_alchemy.purifier.at.itself", "here");
        add("jade.servus_alchemy.purifier.residue", "Caput Mortuum: %s");
        add("jade.servus_alchemy.gnome_hollow.no_vein", "No vein");
        add("jade.servus_alchemy.gnome_hollow.vein", "Vein: %s");
        add("jade.servus_alchemy.gnome_hollow.ripeness", "Ripeness %s / %s");
        add("jade.servus_alchemy.gnome_hollow.ripe", "Ripe, waiting for a gnome");
        add("jade.servus_alchemy.gnome_hollow.gnome", "A gnome is at work");
        add("jade.servus_alchemy.gnome_hollow.store", "Store: %s / %s");
        add("jade.servus_alchemy.gnome_hollow.gift", "Gift: x%s, %s left");
        add(ServusAlchemyBlocks.VAT.get(), "Principle Vat");
        add("message.servus_alchemy.coagulator.formed", "The Coagulator is ready.");
        add("message.servus_alchemy.athanor.formed", "The Athanor is ready.");
        add(ServusAlchemyBlocks.ATHANOR_CROWN.get(), "Athanor");
        add("message.servus_alchemy.athanor.missing_tank",
                "The Athanor is not complete: a Fluid Tank is needed %s, two blocks out. The Athanor page of Paracelsus' Journal shows the build.");
        add("message.servus_alchemy.athanor.missing_duct",
                "The Athanor is not complete: two Copper Ducts are needed one block up toward the tank %s. The Athanor page of Paracelsus' Journal shows the build.");
        add("message.servus_alchemy.athanor.tank.left", "left of it");
        add("message.servus_alchemy.athanor.tank.back", "behind it");
        add("message.servus_alchemy.athanor.tank.right", "right of it");
        add("message.servus_alchemy.athanor.missing",
                "The Athanor needs a hearth: a diamond of 13 Bricks right below it. The Athanor page of Paracelsus' Journal shows it.");
        add("message.servus_alchemy.coagulator.missing",
                "The Coagulator is not complete: %s needed %s. The Coagulator page of Paracelsus' Journal shows the build.");
        add("message.servus_alchemy.coagulator.at.left_of_the_coagulator", "left of the Coagulator");
        add("message.servus_alchemy.coagulator.at.right_of_the_coagulator", "right of the Coagulator");
        add("message.servus_alchemy.coagulator.at.above_the_coagulator", "above the Coagulator");
        add("message.servus_alchemy.coagulator.at.left_of_the_middle_vat", "left of the middle Vat");
        add("message.servus_alchemy.coagulator.at.right_of_the_middle_vat", "right of the middle Vat");
        add("message.servus_alchemy.coagulator.at.itself", "here");
        add("jade.servus_alchemy.purifier.items", "Ash: %s");
        add("jade.servus_alchemy.purifier.water", "%s / %s mB Water");
        add(ServusAlchemyBlocks.CALCINER_STONE_BRICKS.get(), "Calciner Stone Bricks");
        add(ServusAlchemyBlocks.CALCINER_DEEPSLATE_TILES.get(), "Calciner Deepslate Tiles");
        add("message.servus_alchemy.calciner.formed", "The Calciner is ready.");
        add("jade.servus_alchemy.calciner.input", "Matter: %s");
        add("jade.servus_alchemy.calciner.fuel", "Fuel: %s");
        add("jade.servus_alchemy.calciner.progress", "Progress: %s%%");
        add("jade.servus_alchemy.calciner.principle", "%s / %s mB %s");
        add("jade.servus_alchemy.calciner.ash", "Ash: %s / %s");
        add("jade.servus_alchemy.calciner.stack", "%s %s");
        add("jade.servus_alchemy.calciner.empty", "none");
        add("config.jade.plugin_servus_alchemy.calciner", "Calciner");
        add("config.jade.plugin_servus_alchemy.purifier", "Purifier");
        add("config.jade.plugin_servus_alchemy.athanor", "Athanor");
        add("config.jade.plugin_servus_alchemy.gnome_hollow", "Gnome Hollow");
        add("jade.servus_alchemy.athanor.stage", "Stage: %s");
        add("jade.servus_alchemy.athanor.unformed", "Not formed (stage %s)");
        add("jade.servus_alchemy.athanor.input", "Burning: %s");
        add("jade.servus_alchemy.athanor.output", "Made: %s");
        add("jade.servus_alchemy.athanor.principle", "%s mB %s in the tanks");
        add("message.servus_alchemy.calciner.missing",
                "The Calciner is not complete: %s needed %s. The Calciner page of Paracelsus' Journal shows the build.");
        add("message.servus_alchemy.calciner.left_of", "left of the %s");
        add("message.servus_alchemy.calciner.right_of", "right of the %s");
        add("message.servus_alchemy.calciner.above", "above the %s");
        add("message.servus_alchemy.calciner.below", "below the %s");
        add(ServusAlchemyBlocks.COAGULATOR.get(), "Coagulator");
        add("gui.servus_alchemy.coagulator.needs", "%s mB %s");
        add("gui.servus_alchemy.coagulator.select", "Select recipe");
        add("gui.servus_alchemy.coagulator.pick", "Pick a recipe");
        add("gui.servus_alchemy.coagulator.chapter", "Needs the journal chapter %s");
        add("journal.servus_alchemy.villach", "Villach");
        add("journal.servus_alchemy.schwaz", "Schwaz");
        add("journal.servus_alchemy.villach.finished", "Finished the Villach chapter of the journal");
        add("journal.servus_alchemy.schwaz.finished", "Finished the Schwaz chapter of the journal");
        add("journal.servus_alchemy.wandering_years", "Wandering Years");
        add("journal.servus_alchemy.wandering_years.finished", "Finished the Wandering Years chapter of the journal");
        add("journal.servus_alchemy.basel_salzburg", "Basel and Salzburg");
        add("journal.servus_alchemy.basel_salzburg.finished", "Finished the Basel and Salzburg chapter of the journal");
        add("item.servus_alchemy.paracelsus_journal", "Paracelsus' Journal");
        add("advancement.servus_alchemy.journal.root", "Paracelsus' Journal");
        add("advancement.servus_alchemy.journal.root.description", "Experience is the master. Chapters of the journal, finished by doing alchemy");
        add("commands.servus_alchemy.journal.header", "Journal of your team:");
        add("commands.servus_alchemy.journal.chapter", "%s: %s");
        add("commands.servus_alchemy.journal.finished", "finished");
        add("commands.servus_alchemy.journal.open", "open");
        add("commands.servus_alchemy.journal.task", "  %s: %s / %s");
        add("gui.servus_alchemy.jei.calcination", "Calcination");
        add("gui.servus_alchemy.jei.coagulation", "Coagulation");
        add(ServusAlchemyItems.ASH.get(), "Ash");
        add(ServusAlchemyItems.CAPUT_MORTUUM.get(), "Caput Mortuum");
        add(ServusAlchemyItems.SALT_CRYSTAL.get(), "Salt Crystal");
        add("gui.servus_alchemy.jei.purification", "Purification");
        add(ServusAlchemyBlocks.SALT_CRYSTAL_BLOCK.get(), "Salt Crystal Block");
        add(ServusAlchemyBlocks.ATHANOR.get(), "Athanor");
        add(ServusAlchemyBlocks.GNOME_HOLLOW.get(), "Gnome Hollow");
        add(ServusAlchemyBlocks.PRINCIPLE_DROPPER.get(), "Principle Dropper");
        add(ServusAlchemyBlocks.SALAMANDER_FORGE.get(), "Salamander Forge");
        add("message.servus_alchemy.gnome_hollow.bound", "A gnome takes your offering and sets to work.");
        add("message.servus_alchemy.gnome_hollow.no_vein", "Nothing grows here yet. Seed the hollow with a sample of ore.");
        add("message.servus_alchemy.gnome_hollow.not_ripe", "The vein is not ripe yet.");
        add("message.servus_alchemy.gnome_hollow.already_bound", "A gnome already works this vein.");
        add("message.servus_alchemy.gnome_hollow.seeded", "The ore takes root in the hollow.");
        add("message.servus_alchemy.gnome_hollow.already_seeded", "A vein already grows here.");
        add("message.servus_alchemy.gnome_hollow.no_root", "This does not take root here. Not yet, or not at all.");
        add("message.servus_alchemy.gnome_hollow.refused", "The hollow takes none of this.");
        add("message.servus_alchemy.gnome_hollow.ripening", "The vein ripens: %s / %s");
        add("message.servus_alchemy.gnome_hollow.ripe", "The vein is ripe. Offer a Salt Crystal.");
        add("message.servus_alchemy.gnome_hollow.working", "A gnome is at work. Nothing to take yet.");
        add("message.servus_alchemy.gnome_hollow.gift_taken", "The gnome takes your gift and digs with a will.");
        add("message.servus_alchemy.gnome_hollow.gift_disliked", "The gnome wrinkles its nose. It does not want this.");
        add("message.servus_alchemy.gnome_hollow.gift_no_gnome", "No gnome is here to take a gift.");
        add(ElementalSight.ELIXIR.get(), "Elixir of Sight");
        add("message.servus_alchemy.elixir_of_sight.poisoned", "The dose makes the poison! Too much, you fool, too much.");
        add(ElementalSight.EFFECT.get(), "Elemental Sight");
        add("gui.servus_alchemy.athanor.stage.nigredo", "Nigredo");
        add("gui.servus_alchemy.athanor.stage.albedo", "Albedo");
        add("gui.servus_alchemy.athanor.stage.citrinitas", "Citrinitas");
        add("gui.servus_alchemy.athanor.stage.rubedo", "Rubedo");
        add("gui.servus_alchemy.jei.athanor", "Athanor");
        add("gui.servus_alchemy.jei.athanor.tier", "Stage: %s");
        add(ServusAlchemyItems.SULFUR.get(), "Sulfur");
        add(ServusAlchemyItems.QUICKSILVER.get(), "Quicksilver");
        add(ServusAlchemyItems.SAL_AMMONIAC.get(), "Sal Ammoniac");
        add(ServusAlchemyItems.SAL_AMMONIAC.get().getDescriptionId() + ".quote",
                "The salamander gives back what it eats, only better. Do not tell the goldsmiths.");
        add(ServusAlchemyItems.IGNIS_PERPETUUS.get(), "Ignis Perpetuus");
        add(ServusAlchemyItems.IGNIS_PERPETUUS.get().getDescriptionId() + ".quote",
                "Do not ask what it is. It burns forever, that is all you need to know.");
        add(ServusAlchemyItems.SALAMANDER_SPAWN_EGG.get(), "Salamander Spawn Egg");
        add(ServusAlchemyEntities.SALAMANDER.get(), "Salamander");
        for (Principle principle : Principle.values()) {
            String name = PrincipleText.name(principle);
            PrincipleFluids.Entry entry = PrincipleFluids.entry(principle);
            add("fluid_type." + ServusAlchemyIds.MOD_ID + "." + principle.id(), name);
            add(entry.block().get(), name);
            add(entry.bucket().get(), name + " Bucket");
        }
    }
}
