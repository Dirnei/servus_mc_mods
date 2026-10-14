package io.github.dirnei.mc.servus.alchemy.datagen.book;

import com.klikli_dev.modonomicon.api.datagen.CategoryProviderBase;
import com.klikli_dev.modonomicon.api.datagen.EntryBackground;
import com.klikli_dev.modonomicon.api.datagen.EntryProvider;
import com.klikli_dev.modonomicon.api.datagen.book.BookIconModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookCraftingRecipePageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookMultiblockPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookSpotlightPageModel;
import com.klikli_dev.modonomicon.api.datagen.book.page.BookTextPageModel;
import com.klikli_dev.modonomicon.client.gui.book.theme.GuiSprite;
import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyItems;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/** The entries of the journal. Facts are limited to well known parts of Paracelsus' life. */
final class JournalEntries {

    private JournalEntries() {
    }

    /** List lines on a titled page and on every later page; a line often wraps once. */
    static final int LIST_FIRST = 5;
    static final int LIST_REST = 6;

    /** One entry made of text pages, each a title and a body. */
    abstract static class TextEntry extends EntryProvider {

        TextEntry(CategoryProviderBase parent) {
            super(parent);
        }

        /** A titled text, split over as many pages as it needs to stay at full font size. */
        void text(String page, String title, String body) {
            List<String> parts = PageSplitter.split(body);
            for (int i = 0; i < parts.size(); i++) {
                if (i == 0) {
                    page(page, () -> BookTextPageModel.create()
                            .withTitle(context().pageTitle())
                            .withText(context().pageText()));
                    pageTitle(title);
                } else {
                    page(page + "_" + (i + 1), () -> BookTextPageModel.create()
                            .withText(context().pageText()));
                }
                pageText(parts.get(i));
            }
        }

        /** A spotlight page that shows an item with a title and a short text. */
        void spotlight(String page, ItemLike item, String title, String body) {
            page(page, () -> BookSpotlightPageModel.create()
                    .withItem(item)
                    .withTitle(context().pageTitle())
                    .withText(context().pageText()));
            pageTitle(title);
            pageText(body);
        }

        /** A crafting recipe page for a recipe id, with a short text under it. */
        void crafting(String page, String recipeId, String body) {
            page(page, () -> BookCraftingRecipePageModel.create()
                    .withRecipeId1(recipeId)
                    .withText(context().pageText()));
            pageText(body);
        }

        /** Two crafting recipes on one page, with a short text under them. */
        void crafting(String page, String recipeId1, String recipeId2, String body) {
            page(page, () -> BookCraftingRecipePageModel.create()
                    .withRecipeId1(recipeId1)
                    .withRecipeId2(recipeId2)
                    .withText(context().pageText()));
            pageText(body);
        }

        /** A multiblock schematic the reader can turn and project into the world with Visualize. */
        void multiblock(String page, Identifier multiblock, String title, String body) {
            page(page, () -> BookMultiblockPageModel.create()
                    .withMultiblockId(multiblock)
                    .withMultiblockName(context().pageTitle())
                    .withText(context().pageText())
                    .withVisualizeButton(true));
            pageTitle(title);
            pageText(body);
        }

        /** A titled list of lines, laid out over as many pages as it needs at full font size. */
        void list(String page, String title, List<String> lines) {
            list(page, title, lines, LIST_FIRST, LIST_REST);
        }

        /** A titled list with its own line counts, for lines that wrap more than once. */
        void list(String page, String title, List<String> lines, int first, int rest) {
            List<String> parts = PageSplitter.list(lines, first, rest);
            for (int i = 0; i < parts.size(); i++) {
                if (i == 0) {
                    page(page, () -> BookTextPageModel.create()
                            .withTitle(context().pageTitle())
                            .withText(context().pageText()));
                    pageTitle(title);
                } else {
                    page(page + "_" + (i + 1), () -> BookTextPageModel.create()
                            .withText(context().pageText()));
                }
                pageText(parts.get(i));
            }
        }

        abstract ItemLike icon();

        @Override
        protected GuiSprite entryBackground() {
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
                            + "- Wash Ash into Salt in the Purifier\n"
                            + "- Coagulate your first Salt Crystal\n\n"
                            + "Your team shares this work. See your progress with /servus_manufacturing journal.");
            text("athanor", "The Athanor",
                    "Every alchemist needs a furnace that never goes out. Build an Athanor of bricks, a blast furnace and a cauldron. "
                            + "Lay an item on it and feed it principles by bucket or pipe; it burns them into something new.\n\n"
                            + "The stones you set around it decide how far its fire reaches. Alone it burns black, the fire of Nigredo.");
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
            return ServusAlchemyItems.CALCINER.get();
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
                    "Salt is the body of all things. Press nine Salt Crystals into a block, eight such blocks into a ring around your Athanor, "
                            + "and its flame turns white: Albedo.\n\n"
                            + "The spirits of the earth live in the rock as we live in air, and common eyes do not see them. "
                            + "On a white Athanor, Quicksilver and the three principles give the Elixir of Sight.\n\n"
                            + "The miners of Schwaz say the metals grow in the mountain like roots, tended by gnomes. "
                            + "Burn cracked stone bricks with Salt and Mercury on a white Athanor into a Gnome Hollow, give it a sample of ore, "
                            + "and feed it Salt and Mercury until the vein is ripe. Then drink the elixir and offer a Salt Crystal: "
                            + "a gnome takes it and digs that ore for you, for as long as the Hollow stands.\n\n"
                            + "To go on: finish one craft on an Albedo Athanor, drink the Elixir of Sight, and bind a gnome.");
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
