package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.resources.Identifier;

/** Paracelsus' Journal: the Modonomicon book that is Alchemy's tech tree. */
public final class JournalBook extends SingleBookSubProvider {

    public static final String ID = "journal";

    public JournalBook() {
        super(ID, ServusAlchemyIds.MOD_ID);
    }

    @Override
    protected void registerDefaultMacros() {
    }

    @Override
    protected void generateCategories() {
        add(new OpusCategory(this).generate());
        add(new WorkshopCategory(this).generate());
    }

    @Override
    protected BookModel additionalSetup(BookModel book) {
        return book.withGenerateBookItem(false)
                .withCustomBookItem(Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "paracelsus_journal"))
                // An inner margin keeps the text clear of the page edges.
                .withTheme(theme -> theme.withLayout(layout -> layout.withBookTextOffsetX(7)
                        .withBookTextOffsetY(4)
                        .withBookTextOffsetWidth(-14)
                        .withBookTextOffsetHeight(-8)))
                .withModel(Identifier.fromNamespaceAndPath("modonomicon", "modonomicon_red"))
                .withCreativeTab(Identifier.fromNamespaceAndPath("servus_foundation", "servus"));
    }

    @Override
    protected String bookName() {
        return "Paracelsus' Journal";
    }

    @Override
    protected String bookTooltip() {
        return "Notes of Theophrastus von Hohenheim. Experience is the master.";
    }
}
