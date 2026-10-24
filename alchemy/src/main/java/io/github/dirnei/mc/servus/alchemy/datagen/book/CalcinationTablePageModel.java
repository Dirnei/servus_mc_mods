package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.book.page.BookPage;
import io.github.dirnei.mc.servus.alchemy.journal.CalcinationTablePage;
import net.minecraft.core.HolderLookup;

/** Datagen model of {@link CalcinationTablePage}. */
final class CalcinationTablePageModel extends BookPageModel<CalcinationTablePageModel> {

    private final int part;
    private final boolean last;

    private CalcinationTablePageModel(int part, boolean last) {
        super(CalcinationTablePage.ID);
        this.part = part;
        this.last = last;
    }

    static CalcinationTablePageModel of(int part, boolean last) {
        return new CalcinationTablePageModel(part, last);
    }

    @Override
    public BookPage toBookPage(HolderLookup.Provider provider) {
        return new CalcinationTablePage(id, condition(provider), part, last);
    }
}
