package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.book.page.BookPageModel;
import com.klikli_dev.modonomicon.book.page.BookPage;
import io.github.dirnei.mc.servus.alchemy.journal.JournalTasksPage;
import net.minecraft.core.HolderLookup;

/** Datagen model of {@link JournalTasksPage}. */
final class JournalTasksPageModel extends BookPageModel<JournalTasksPageModel> {

    private final String chapter;

    private JournalTasksPageModel(String chapter) {
        super(JournalTasksPage.ID);
        this.chapter = chapter;
    }

    static JournalTasksPageModel of(String chapter) {
        return new JournalTasksPageModel(chapter);
    }

    @Override
    public BookPage toBookPage(HolderLookup.Provider provider) {
        return new JournalTasksPage(id, condition(provider), chapter);
    }
}
