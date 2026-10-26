package io.github.dirnei.mc.servus.foundation.client;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.picker.PickerStock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.lwjgl.glfw.GLFW;

/**
 * An item picker for container screens: a button that shows the current
 * choice and opens a popup over the GUI with a search field and a scrollable
 * list of entries, each with its icon, name and, when stock counts are set, a
 * count. Entries with no stock stay selectable but are greyed and show the
 * zero hint. A "none" entry ({@link PickerList#NONE_ID}) always stays first.
 *
 * <p>Use: create it with a title and a pick callback, give it entries, a
 * button rectangle and the area to centre on (all absolute screen
 * coordinates, set them again in {@code init}), then call {@link #renderButton}
 * from {@code extractBackground}, {@link #renderPopup} and {@link #renderTooltip}
 * at the end of {@code extractRenderState}, and forward the mouse and key events
 * first, stopping when they return true. While {@link #isOpen()} the screen
 * should pass {@code -1, -1} as the mouse to {@code super.extractRenderState}
 * and skip its own tooltips.
 *
 * <p>Without a button: skip {@link #setButton} and {@link #renderButton} and
 * open the popup with {@link #open(String)} from the screen's own click, for
 * example on a slot; the screen remembers what the pick is for.
 */
public final class ItemPicker {

    private static final int ROW = 18;
    private static final int ROWS = 7;
    private static final int PAD = 6;
    private static final int TITLE_HEIGHT = 12;
    private static final int SEARCH_HEIGHT = 12;
    private static final int SEARCH_GAP = 4;
    private static final int LIST_WIDTH = 140;
    private static final int BAR_GAP = 3;
    private static final int BAR_WIDTH = 6;
    private static final int WIDTH = PAD + LIST_WIDTH + BAR_GAP + BAR_WIDTH + PAD;
    private static final int HEIGHT = TITLE_HEIGHT + PAD + SEARCH_HEIGHT + SEARCH_GAP + ROWS * ROW + PAD;
    private static final int BUTTON_HEIGHT = 18;
    private static final int TEXT = 0xFF202020;
    private static final int GREY = 0xFF707070;
    private static final int SELECTED = 0xFF8FC08F;
    private static final int HOVERED = 0x60FFFFFF;
    private static final int DARK = 0xFF373737;
    private static final int SLOT = 0xFF8B8B8B;

    /**
     * One entry: an id (an item or fluid id, or {@link PickerList#NONE_ID}), its icon (may be empty), a fluid
     * drawn as its texture when the icon is empty (may be empty), and its name.
     */
    public record Entry(String id, ItemStack icon, FluidStack fluid, Component name) {

        public Entry(String id, ItemStack icon, Component name) {
            this(id, icon, FluidStack.EMPTY, name);
        }

        public static Entry none(Component name) {
            return new Entry(PickerList.NONE_ID, ItemStack.EMPTY, name);
        }

        /** An entry for a fluid, shown with its still texture in its colour. */
        public static Entry fluid(String id, FluidStack fluid, Component name) {
            return new Entry(id, ItemStack.EMPTY, fluid, name);
        }

        public boolean isNone() {
            return PickerList.NONE_ID.equals(id);
        }

        /** The entry's item id, or empty for the none entry or an id that does not parse. */
        public Optional<Identifier> itemId() {
            return identifier();
        }

        /** The entry's fluid id, or empty for the none entry or an id that does not parse. */
        public Optional<Identifier> fluidId() {
            return identifier();
        }

        private Optional<Identifier> identifier() {
            return isNone() ? Optional.empty() : Optional.ofNullable(Identifier.tryParse(id));
        }

        /** Draws the icon, or the fluid when there is no icon, at a 16 by 16 spot. */
        void drawIcon(GuiGraphicsExtractor graphics, int x, int y) {
            if (!icon.isEmpty()) {
                graphics.item(icon, x, y);
            } else if (!fluid.isEmpty()) {
                FluidSprites.drawIcon(graphics, fluid, x, y);
            }
        }

        boolean hasIcon() {
            return !icon.isEmpty() || !fluid.isEmpty();
        }

        /** The entry id of a stack's item; {@link PickerList#NONE_ID} for an empty stack. */
        public static String idOf(ItemStack stack) {
            return stack.isEmpty() ? PickerList.NONE_ID : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }
    }

    private final Component title;
    private final Consumer<Entry> onPick;
    private final Map<String, Entry> byId = new HashMap<>();
    private List<PickerList.Item> sorted = List.of();
    private List<Entry> shown = List.of();
    private String selected = PickerList.NONE_ID;
    private Map<String, Long> stock;
    private Component zeroHint = Component.empty();
    private Map<String, Component> locked = Map.of();
    private int buttonX;
    private int buttonY;
    private int buttonWidth;
    private int areaLeft;
    private int areaTop;
    private int areaWidth;
    private int areaHeight;
    private boolean open;
    private int x;
    private int y;
    private int scroll;
    private boolean dragging;
    private EditBox search;

    /** A picker with that popup title; {@code onPick} gets the entry the player clicks. */
    public ItemPicker(Component title, Consumer<Entry> onPick) {
        this.title = title;
        this.onPick = onPick;
    }

    /** The entries to choose from, in any order; they are shown in {@link PickerList#ranked} order, set when the list opens or the search changes. */
    public void setEntries(List<Entry> entries) {
        byId.clear();
        List<PickerList.Item> items = new ArrayList<>();
        for (Entry entry : entries) {
            byId.put(entry.id(), entry);
            items.add(new PickerList.Item(entry.id(), entry.name().getString()));
        }
        sorted = PickerList.sorted(items);
        refilter();
    }

    /** The id of the current choice; {@link PickerList#NONE_ID} for the none entry. */
    public void setSelected(String id) {
        selected = id == null ? PickerList.NONE_ID : id;
    }

    /** Stock counts by entry id, ids missing hold none; null shows no counts and greys nothing. */
    public void setStock(Map<String, Long> counts) {
        stock = counts;
    }

    /** The hint shown on an entry with no stock. */
    public void setZeroHint(Component hint) {
        zeroHint = hint;
    }

    /** Entries that cannot be picked now, by id, with the hint that says why; they are drawn grey and ignore clicks. */
    public void setLocked(Map<String, Component> hints) {
        locked = Map.copyOf(hints);
    }

    /** Where the button sits, absolute; 18 pixels tall. */
    public void setButton(int x, int y, int width) {
        buttonX = x;
        buttonY = y;
        buttonWidth = width;
    }

    /** The GUI area the popup centres on and dims, absolute. */
    public void setArea(int left, int top, int width, int height) {
        areaLeft = left;
        areaTop = top;
        areaWidth = width;
        areaHeight = height;
        x = left + (width - WIDTH) / 2;
        y = top + (height - HEIGHT) / 2;
        if (search != null) {
            search.setPosition(x + PAD, y + TITLE_HEIGHT + PAD);
        }
    }

    public boolean isOpen() {
        return open;
    }

    /** Opens the popup with {@code selectedId} as the current choice, scrolled to it. */
    public void open(String selectedId) {
        setSelected(selectedId);
        open();
    }

    /** Opens the popup with the current choice, scrolled to it. */
    public void open() {
        open = true;
        dragging = false;
        search = new EditBox(Minecraft.getInstance().font, x + PAD, y + TITLE_HEIGHT + PAD, LIST_WIDTH + BAR_GAP + BAR_WIDTH, SEARCH_HEIGHT,
                Component.translatable("gui.servus_foundation.picker.search"));
        search.setHint(Component.translatable("gui.servus_foundation.picker.search"));
        search.setMaxLength(50);
        search.setResponder(text -> refilter());
        search.setFocused(true);
        refilter();
        int index = indexOf(selected);
        scroll = Math.max(0, Math.min(maxScroll(), index - ROWS / 2));
    }

    public void close() {
        open = false;
        dragging = false;
        search = null;
        refilter();
    }

    private void refilter() {
        String query = search == null ? "" : search.getValue();
        List<Entry> entries = new ArrayList<>();
        for (PickerList.Item item : PickerList.ranked(PickerList.filter(sorted, query), locked.keySet(), stock)) {
            entries.add(byId.get(item.id()));
        }
        shown = entries;
        scroll = Math.min(scroll, maxScroll());
    }

    private int indexOf(String id) {
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /** The stock of an entry, or -1 when no count is shown for it. */
    private long count(Entry entry) {
        if (stock == null || entry.isNone()) {
            return -1;
        }
        return stock.getOrDefault(entry.id(), 0L);
    }

    private Entry current() {
        Entry entry = byId.get(selected);
        return entry != null ? entry : byId.get(PickerList.NONE_ID);
    }

    /** The button with the current choice; call from {@code renderBg}. */
    public void renderButton(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        boolean hovered = !open && overButton(mouseX, mouseY);
        int right = buttonX + buttonWidth;
        PopupPanel.bordered(graphics, buttonX + 1, buttonY + 1, buttonWidth - 2, BUTTON_HEIGHT - 2, DARK,
                hovered ? 0xFFDDDDDD : 0xFFA0A0A0);
        graphics.fill(buttonX + 1, buttonY + 1, right - 1, buttonY + 2, 0xFFFFFFFF);
        Entry entry = current();
        int textX = buttonX + 4;
        if (entry != null && entry.hasIcon()) {
            entry.drawIcon(graphics, buttonX + 1, buttonY + 1);
            textX = buttonX + 20;
        }
        int textRight = right - 10;
        if (entry != null) {
            long count = count(entry);
            if (count >= 0) {
                String text = PickerStock.shortCount(count);
                textRight -= font.width(text) + 4;
                graphics.text(font, text, right - 10 - font.width(text), buttonY + 5, count == 0 ? GREY : TEXT, false);
            }
            String name = font.plainSubstrByWidth(entry.name().getString(), textRight - textX);
            graphics.text(font, name, textX, buttonY + 5, count == 0 ? GREY : TEXT, false);
        }
        graphics.text(font, "v", right - 8, buttonY + 5, TEXT, false);
    }

    /** The popup, while open, over everything; call at the end of {@code render}. */
    public void renderPopup(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }
        PopupPanel.draw(graphics, areaLeft, areaTop, areaWidth, areaHeight, x, y, WIDTH, HEIGHT);
        graphics.text(font, title, x + PAD, y + 5, 0xFF404040, false);
        search.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int lx = listX();
        int ly = listY();
        PopupPanel.bordered(graphics, lx, ly, LIST_WIDTH, ROWS * ROW, DARK, SLOT);
        int hovered = indexAt(mouseX, mouseY);
        for (int row = 0; row < ROWS; row++) {
            int index = scroll + row;
            if (index >= shown.size()) {
                break;
            }
            Entry entry = shown.get(index);
            int top = ly + row * ROW;
            if (entry.id().equals(selected)) {
                graphics.fill(lx, top, lx + LIST_WIDTH, top + ROW, SELECTED);
            }
            entry.drawIcon(graphics, lx + 1, top + 1);
            long count = count(entry);
            int colour = count == 0 || locked.containsKey(entry.id()) ? GREY : TEXT;
            int nameRight = lx + LIST_WIDTH - 3;
            if (count >= 0) {
                String text = PickerStock.shortCount(count);
                nameRight -= font.width(text) + 4;
                graphics.text(font, text, lx + LIST_WIDTH - 3 - font.width(text), top + 5, colour, false);
            }
            int textX = lx + 20;
            graphics.text(font, font.plainSubstrByWidth(entry.name().getString(), nameRight - textX), textX, top + 5, colour, false);
            if (index == hovered) {
                graphics.fill(lx, top, lx + LIST_WIDTH, top + ROW, HOVERED);
            }
        }
        int max = maxScroll();
        if (max > 0) {
            int bx = barX();
            graphics.fill(bx, ly, bx + BAR_WIDTH, ly + ROWS * ROW, DARK);
            int knob = knobHeight();
            int ky = ly + (ROWS * ROW - knob) * scroll / max;
            graphics.fill(bx + 1, ky + 1, bx + BAR_WIDTH - 1, ky + knob - 1, 0xFFDDDDDD);
        }
    }

    /**
     * The tooltip of the hovered entry (its name, and the zero hint when it has
     * no stock), or of the button when its choice has no stock. Returns whether
     * one was drawn.
     */
    public boolean renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        Entry entry = null;
        if (open) {
            int index = indexAt(mouseX, mouseY);
            entry = index < 0 ? null : shown.get(index);
        } else if (overButton(mouseX, mouseY)) {
            entry = current();
        }
        if (entry == null) {
            return open;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(entry.name());
        Component lockHint = locked.get(entry.id());
        if (lockHint != null) {
            lines.add(lockHint.copy().withColor(0xA04000));
        } else if (count(entry) == 0) {
            lines.add(zeroHint.copy().withColor(0xA04000));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    /** Opens on a button click; while open, picks, drags the bar, focuses the search or closes on a click outside. */
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (!open) {
            if (button == 0 && overButton(mouseX, mouseY)) {
                open();
                return true;
            }
            return false;
        }
        if (search.mouseClicked(event, doubleClick)) {
            search.setFocused(true);
            return true;
        }
        int index = indexAt(mouseX, mouseY);
        if (index >= 0) {
            Entry entry = shown.get(index);
            if (locked.containsKey(entry.id())) {
                return true;
            }
            selected = entry.id();
            close();
            onPick.accept(entry);
            return true;
        }
        if (clickBar(mouseX, mouseY)) {
            return true;
        }
        if (!contains(mouseX, mouseY)) {
            close();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!open) {
            return false;
        }
        drag(mouseY);
        return true;
    }

    public boolean mouseReleased() {
        if (!open) {
            return false;
        }
        dragging = false;
        return true;
    }

    public boolean mouseScrolled(double amount) {
        if (!open) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(amount)));
        return true;
    }

    /** Escape closes the popup only; every other key goes to the search while open. */
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (!open) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        search.keyPressed(event);
        return true;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!open) {
            return false;
        }
        search.charTyped(event);
        return true;
    }

    private boolean overButton(double mouseX, double mouseY) {
        return mouseX >= buttonX && mouseX < buttonX + buttonWidth && mouseY >= buttonY && mouseY < buttonY + BUTTON_HEIGHT;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }

    private int indexAt(double mouseX, double mouseY) {
        int lx = listX();
        int ly = listY();
        if (!open || mouseX < lx || mouseX >= lx + LIST_WIDTH || mouseY < ly || mouseY >= ly + ROWS * ROW) {
            return -1;
        }
        int index = scroll + (int) ((mouseY - ly) / ROW);
        return index < shown.size() ? index : -1;
    }

    private boolean clickBar(double mouseX, double mouseY) {
        int bx = barX();
        int ly = listY();
        if (maxScroll() > 0 && mouseX >= bx && mouseX < bx + BAR_WIDTH && mouseY >= ly && mouseY < ly + ROWS * ROW) {
            dragging = true;
            drag(mouseY);
            return true;
        }
        return false;
    }

    private void drag(double mouseY) {
        if (!dragging) {
            return;
        }
        int max = maxScroll();
        int track = ROWS * ROW - knobHeight();
        double t = track <= 0 ? 0 : (mouseY - listY() - knobHeight() / 2.0) / track;
        scroll = Math.max(0, Math.min(max, (int) Math.round(t * max)));
    }

    private int knobHeight() {
        return Math.max(8, ROWS * ROW * ROWS / Math.max(ROWS, shown.size()));
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - ROWS);
    }

    private int listX() {
        return x + PAD;
    }

    private int listY() {
        return y + TITLE_HEIGHT + PAD + SEARCH_HEIGHT + SEARCH_GAP;
    }

    private int barX() {
        return listX() + LIST_WIDTH + BAR_GAP;
    }
}
