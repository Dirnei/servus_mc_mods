package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import java.util.function.Consumer;
import net.minecraft.world.level.ItemLike;

/** One tutorial entry of the Workshop category; its pages come from a function so the category stays one readable list. */
final class WorkshopEntry extends JournalEntries.TextEntry {

    private final String id;
    private final String name;
    private final String description;
    private final ItemLike icon;
    private final Consumer<WorkshopEntry> pages;

    WorkshopEntry(CategoryProviderBase parent, String id, String name, String description, ItemLike icon,
                  Consumer<WorkshopEntry> pages) {
        super(parent);
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.pages = pages;
    }

    @Override
    protected void generatePages() {
        pages.accept(this);
    }

    @Override
    protected String entryName() {
        return name;
    }

    @Override
    protected String entryDescription() {
        return description;
    }

    @Override
    ItemLike icon() {
        return icon;
    }

    @Override
    protected String entryId() {
        return id;
    }
}
