package io.github.dirnei.mc.servus.notes.client;

import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.FADED_INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.INK;
import static io.github.dirnei.mc.servus.notes.client.NotebookScreen.PAGE_MARGIN;

import io.github.dirnei.mc.servus.notes.model.ItemQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;

/**
 * The item picker over the right page: a search field and a grid of the matching items. A click on
 * an item, or an item dropped from JEI onto the grid, hands its id to whoever opened the picker.
 */
final class ItemPickerPopup {

    private static final int INSET = 4;
    private static final int CELL = 18;
    private static final int MAX_MATCHES = 200;
    private static final int PAPER = 0xFFF1E4C3;
    private static final int POPUP_EDGE = 0xFFC9B07A;
    private static final int CELL_HOVER = 0xFFE5D0A0;
    private static final int CELL_BACK = 0xFFE9DCBC;

    private record Entry(Item item, ItemStack stack, String name, String namespace, String modName) {
    }

    private final NotebookScreen screen;
    private EditBox search;
    private List<Entry> all = List.of();
    private List<Entry> matches = List.of();
    private int scroll;
    private @Nullable Component heading;
    private @Nullable Consumer<String> onPick;

    ItemPickerPopup(NotebookScreen screen) {
        this.screen = screen;
    }

    void init(Font font, Consumer<AbstractWidget> add) {
        String text = search == null ? "" : search.getValue();
        search = new EditBox(font, 0, 0, 10, 9, Component.translatable("gui.servus_notes.notebook.picker.search"));
        search.setBordered(false);
        search.setTextColor(INK);
        search.setTextShadow(false);
        search.setInvertHighlightedTextColor(false);
        search.setHint(Component.translatable("gui.servus_notes.notebook.picker.search_hint").withColor(FADED_INK & 0xFFFFFF).withoutShadow());
        search.setValue(text);
        search.setResponder(value -> filter());
        search.setVisible(isOpen());
        add.accept(search);
    }

    boolean isOpen() {
        return onPick != null;
    }

    /** Opens the picker under {@code title}; the picked item's id goes to {@code pick}, after which the picker closes. */
    void open(Component title, Consumer<String> pick) {
        heading = title;
        onPick = pick;
        all = entries();
        search.setValue("");
        search.setVisible(true);
        screen.setFocused(search);
        filter();
    }

    void close() {
        onPick = null;
        heading = null;
        all = List.of();
        matches = List.of();
        if (search != null) {
            search.setVisible(false);
            if (screen.getFocused() == search) {
                screen.setFocused(null);
            }
        }
    }

    private static List<Entry> entries() {
        Map<String, String> modNames = new HashMap<>();
        List<Entry> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemStack stack = item.getDefaultInstance();
            String namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            String modName = modNames.computeIfAbsent(namespace, ns -> ModList.get().getModContainerById(ns)
                    .map(container -> container.getModInfo().getDisplayName()).orElse(ns));
            entries.add(new Entry(item, stack, stack.getHoverName().getString(), namespace, modName));
        }
        return entries;
    }

    private void filter() {
        ItemQuery query = ItemQuery.of(search.getValue());
        matches = all.stream()
                .filter(entry -> query.matches(entry.name(), entry.namespace(), entry.modName()))
                .limit(MAX_MATCHES)
                .toList();
        scroll = 0;
    }

    void extract(GuiGraphicsExtractor graphics, Font font, BookLayout book, double mouseX, double mouseY) {
        int popupX0 = book.rightPageX() + INSET;
        int popupX1 = book.rightPageX() + book.rightPageWidth() - INSET;
        int popupY0 = book.pageY() + INSET;
        int popupY1 = book.pageBottom() - INSET;
        int x0 = book.rightPageX() + PAGE_MARGIN + 2;
        int x1 = book.rightPageX() + book.rightPageWidth() - PAGE_MARGIN;
        screen.hits().add(book.rightPageX(), book.pageY(), book.rightPageWidth(), book.pageHeight(), () -> { });
        graphics.fill(popupX0, popupY0, popupX1, popupY1, PAPER);
        graphics.outline(popupX0, popupY0, popupX1 - popupX0, popupY1 - popupY0, POPUP_EDGE);
        int headingY = popupY0 + 6;
        if (heading != null) {
            graphics.text(font, NotebookScreen.ellipsized(font, heading, x1 - x0), x0, headingY, INK, false);
        }
        int searchY = headingY + 13;
        PaperButton.drawField(graphics, x0, searchY, x1 - x0, PaperButton.FIELD_HEIGHT, screen.getFocused() == search);
        search.setX(x0 + 3);
        search.setY(searchY + 3);
        TaskPage.fitWidth(search, x1 - x0 - 6);
        int gridY = searchY + PaperButton.FIELD_HEIGHT + 5;
        int buttonY = popupY1 - PaperButton.HEIGHT - 4;
        int columns = Math.max(1, (x1 - x0) / CELL);
        int rows = Math.max(1, (buttonY - 4 - gridY) / CELL);
        int gridX = x0 + (x1 - x0 - columns * CELL) / 2;
        int allRows = Math.ceilDiv(matches.size(), columns);
        scroll = Mth.clamp(scroll, 0, Math.max(0, allRows - rows));
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = (scroll + row) * columns + column;
                if (index >= matches.size()) {
                    break;
                }
                extractCell(graphics, matches.get(index), gridX + column * CELL, gridY + row * CELL, mouseX, mouseY);
            }
        }
        if (matches.isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_notes.notebook.picker.none"), x0, gridY + 4, FADED_INK, false);
        }
        screen.itemDrops().add(new ItemDrop(gridX, gridY, columns * CELL, rows * CELL, this::pick));
        Component cancel = Component.translatable("gui.servus_notes.notebook.cancel");
        int cancelX = x1 - PaperButton.width(font, cancel);
        screen.button(graphics, cancel, cancelX, buttonY, mouseX, mouseY, INK, this::close);
        Component found = Component.translatable(matches.size() >= MAX_MATCHES
                ? "gui.servus_notes.notebook.picker.many" : "gui.servus_notes.notebook.picker.found", matches.size());
        if (ModList.get().isLoaded("jei")) {
            found = Component.translatable("gui.servus_notes.notebook.picker.with_jei", found);
        }
        graphics.text(font, NotebookScreen.ellipsized(font, found, cancelX - x0 - 4), x0, buttonY + 2, FADED_INK, false);
    }

    private void extractCell(GuiGraphicsExtractor graphics, Entry entry, int x, int y, double mouseX, double mouseY) {
        boolean hovered = PageHits.over(mouseX, mouseY, x, y, CELL, CELL);
        graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, hovered ? CELL_HOVER : CELL_BACK);
        if (hovered) {
            screen.tooltip(entry.stack());
        }
        graphics.item(entry.stack(), x + 1, y + 1);
        String id = GoalItems.idOf(entry.item());
        screen.hits().add(x, y, CELL, CELL, () -> pick(id));
    }

    private void pick(String itemId) {
        Consumer<String> pick = onPick;
        close();
        if (pick != null) {
            pick.accept(itemId);
        }
    }

    /** Scrolls the grid by whole rows. */
    boolean scrolled(double amount) {
        scroll -= (int) Math.signum(amount);
        return true;
    }

    /** Escape closes the picker; false when it is not open. */
    boolean escape() {
        if (!isOpen()) {
            return false;
        }
        close();
        return true;
    }
}
