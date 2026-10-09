package io.github.dirnei.mc.servus.manufacturing.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.mojang.datafixers.util.Pair;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingItems;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** The entries of the journal. Story text is a first draft; facts are limited to well known parts of his life. */
final class JournalEntries {

    private JournalEntries() {
    }

    /** One entry made of text pages, each a title and a body. */
    abstract static class TextEntry extends EntryProvider {

        TextEntry(CategoryProviderBase parent) {
            super(parent);
        }

        void text(String page, String title, String body) {
            page(page, () -> BookTextPageModel.create()
                    .withTitle(context().pageTitle())
                    .withText(context().pageText()));
            pageTitle(title);
            pageText(body);
        }

        abstract ItemLike icon();

        @Override
        protected Pair<Integer, Integer> entryBackground() {
            return EntryBackground.DEFAULT;
        }

        @Override
        protected BookIconModel entryIcon() {
            return BookIconModel.create(icon());
        }
    }

    static final class Preface extends TextEntry {

        Preface(CategoryProviderBase parent) {
            super(parent);
        }

        @Override
        protected void generatePages() {
            text("intro", "To the Reader",
                    "These are the notes of Theophrastus von Hohenheim, who called himself Paracelsus.\n\n"
                            + "I learned from the furnace, the mine and the sickbed, not from old books. "
                            + "Experience is the master. Follow these pages by doing the work, and each chapter of my life will open to you.");
        }

        @Override
        protected String entryName() {
            return "To the Reader";
        }

        @Override
        protected String entryDescription() {
            return "How to read this journal";
        }

        @Override
        ItemLike icon() {
            return Items.WRITABLE_BOOK;
        }

        @Override
        protected String entryId() {
            return "preface";
        }
    }

    static final class Villach extends TextEntry {

        Villach(CategoryProviderBase parent) {
            super(parent);
        }

        @Override
        protected void generatePages() {
            text("story", "Villach",
                    "My father was a physician in Villach, in Carinthia, a country of mines and smelters. "
                            + "There I first watched ore turn into metal in the fire.\n\n"
                            + "Every body is made of three: Salt, which gives it form, Sulfur, which burns, and Mercury, the spirit that flies off. "
                            + "Burn a thing in the Calciner and the three part from each other. Join them again in the Coagulator.");
            text("tasks", "The Work",
                    "To finish this chapter:\n\n"
                            + "- Calcine 100 things\n"
                            + "- Gather 1,000 mB of Salt\n"
                            + "- Coagulate your first Salt Crystal\n\n"
                            + "Your team shares this work. See your progress with /servus_manufacturing journal.");
            text("unlocks", "What Follows",
                    "Finishing Villach opens the next chapter of the journal: Schwaz, and the mines.");
        }

        @Override
        protected String entryName() {
            return "Villach";
        }

        @Override
        protected String entryDescription() {
            return "Nigredo: the first fire";
        }

        @Override
        ItemLike icon() {
            return ServusManufacturingItems.CALCINER.get();
        }

        @Override
        protected String entryId() {
            return "villach";
        }
    }

    static final class Schwaz extends TextEntry {

        Schwaz(CategoryProviderBase parent) {
            super(parent);
        }

        @Override
        protected void generatePages() {
            text("story", "Schwaz",
                    "In the silver mines of Schwaz in Tyrol I worked among the miners and saw the sickness the mountain gives them.\n\n"
                            + "The old miners say that metals grow in the rock like plants, and that small spirits of the earth live in the galleries. "
                            + "If the mountain grows its ores, perhaps it can be asked to.");
            text("tasks", "The Work",
                    "The spirits of the earth have not shown themselves yet. This chapter continues in a later edition of the journal.");
        }

        @Override
        protected String entryName() {
            return "Schwaz";
        }

        @Override
        protected String entryDescription() {
            return "Albedo: the mountain answers";
        }

        @Override
        ItemLike icon() {
            return Items.IRON_PICKAXE;
        }

        @Override
        protected String entryId() {
            return "schwaz";
        }
    }
}
