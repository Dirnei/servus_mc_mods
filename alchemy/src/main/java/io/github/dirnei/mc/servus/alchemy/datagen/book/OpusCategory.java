package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconProviderBase;
import com.klikli_dev.modonomicon.api.datagen.book.BookEntryModel;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import net.minecraft.world.item.Items;

/** The one category of the journal: the Great Work, chapter after chapter. */
final class OpusCategory extends CategoryProvider {

    OpusCategory(ModonomiconProviderBase parent) {
        super(parent);
    }

    @Override
    protected String[] generateEntryMap() {
        return new String[] {
                "_________________________",
                "_____p___v___s___w___b___",
                "_____________o___________",
        };
    }

    @Override
    protected void generateEntries() {
        BookEntryModel preface = add(new JournalEntries.Preface(this).generate('p'));
        BookEntryModel villach = new JournalEntries.Villach(this).generate('v');
        villach.withParent(preface);
        add(villach);
        BookEntryModel schwaz = new JournalEntries.Schwaz(this).generate('s');
        schwaz.withParent(villach);
        schwaz.withCondition(condition().researchNodeUnlocked(JournalResearch.VILLACH));
        add(schwaz);
        BookEntryModel oldGnome = new JournalEntries.OldGnome(this).generate('o');
        oldGnome.withParent(schwaz);
        oldGnome.withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ));
        add(oldGnome);
        BookEntryModel wandering = new JournalEntries.WanderingYears(this).generate('w');
        wandering.withParent(schwaz);
        wandering.withCondition(condition().researchNodeUnlocked(JournalResearch.SCHWAZ));
        add(wandering);
        BookEntryModel basel = new JournalEntries.BaselSalzburg(this).generate('b');
        basel.withParent(wandering);
        basel.withCondition(condition().researchNodeUnlocked(JournalResearch.WANDERING_YEARS));
        add(basel);
    }

    @Override
    protected String categoryName() {
        return "The Great Work";
    }

    @Override
    protected BookIconModel categoryIcon() {
        return BookIconModel.create(Items.WRITABLE_BOOK);
    }

    @Override
    public String categoryId() {
        return "opus";
    }
}
