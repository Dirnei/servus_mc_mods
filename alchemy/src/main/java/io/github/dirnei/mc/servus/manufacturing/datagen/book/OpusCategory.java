package io.github.dirnei.mc.servus.manufacturing.datagen.book;

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
                "_____________________",
                "_____p___v___s_______",
                "_____________________",
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
