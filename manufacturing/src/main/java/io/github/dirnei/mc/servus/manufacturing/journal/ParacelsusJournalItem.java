package io.github.dirnei.mc.servus.manufacturing.journal;

import com.klikli_dev.modonomicon.item.ModonomiconCustomItemBase;

/**
 * Paracelsus' Journal as Manufacturing's own item, so it reads as a Servus item instead of a
 * Modonomicon one. Modonomicon's base sets the book id on the stack and opens the journal book.
 */
public class ParacelsusJournalItem extends ModonomiconCustomItemBase {

    public ParacelsusJournalItem(Properties properties) {
        super(JournalBookItem.BOOK, properties);
    }
}
