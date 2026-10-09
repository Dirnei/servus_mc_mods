package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.SingleBookSubProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookModel;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

/** Paracelsus' Journal: the Modonomicon book that is Manufacturing's tech tree. */
public final class JournalBook extends SingleBookSubProvider {

    public static final String ID = "journal";

    public JournalBook(BiConsumer<String, String> lang) {
        super(ID, ServusManufacturingIds.MOD_ID, lang);
    }

    @Override
    protected void registerDefaultMacros() {
    }

    @Override
    protected void generateCategories() {
        add(new OpusCategory(this).generate());
    }

    @Override
    protected BookModel additionalSetup(BookModel book) {
        // An inner margin so text keeps clear of the page edges.
        // The journal is Manufacturing's own item, so it reads as a Servus item, not a Modonomicon one.
        return book.withGenerateBookItem(false)
                .withCustomBookItem(ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "paracelsus_journal"))
                .withBookTextOffsetX(7)
                .withBookTextOffsetY(4)
                .withBookTextOffsetWidth(-14)
                .withBookTextOffsetHeight(-8)
                .withModel(ResourceLocation.fromNamespaceAndPath("modonomicon", "modonomicon_red"))
                .withCreativeTab(ResourceLocation.fromNamespaceAndPath("servus_foundation", "servus"));
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
