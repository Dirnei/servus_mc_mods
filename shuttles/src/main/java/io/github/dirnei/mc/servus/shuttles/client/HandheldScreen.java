package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.PickerEntries;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.picker.TagSearch;
import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.handheld.Basket;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBasketPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldLockersPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldPickerPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRequestPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocatePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldRelocation;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZoneActionPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZonePayload;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesComponents;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneDefinition;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The handheld's GUI, drawn as the device: a yellow bumpered body with the
 * red scan window, the brand line and a green CRT screen with scanlines, a
 * vignette and a slow glow. The screen opens on the view of the handheld's
 * mode; three softkeys below it switch the mode. ZONE lists the team's zones
 * (choose the active one, new, edit, delete with a second click when loaded
 * shelves use it) and opens the zone form, whose filter is picked in the item
 * picker with {@code #} tag search. INFO shows the readout of the last
 * scanned shelf above its items and the shelf basket, with Clear and
 * Relocate, which waits for the target shelf; a Fluid Rack shelf shows only
 * its readout. STOCK lists the bound warehouse's stock above the warehouse
 * basket, with Clear and Request, which opens the locker picker: the
 * warehouse's Parcel Lockers nearest first, the nearest that takes the order
 * selected, with Send and Cancel. In both lists a click on an item adds it to the basket
 * through an amount popup (shift click adds a stack), a right click lights up
 * its shelves; a click on a basket cell edits its amount, a right click
 * removes it.
 */
public class HandheldScreen extends AbstractContainerScreen<HandheldMenu> {

    private static final String KEY = "gui.servus_shuttles.handheld.";
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/handheld.png");
    private static final Identifier SCANLINES = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/handheld_scanlines.png");
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/handheld_vignette.png");
    private static final int WIDTH = 216;
    private static final int HEIGHT = 236;
    private static final int SCREEN_X = 14;
    private static final int SCREEN_Y = 30;
    private static final int SCREEN_W = 188;
    private static final int SCREEN_H = 160;
    private static final int KEY_Y = 196;
    private static final int KEY_W = 56;
    private static final int KEY_H = 20;
    private static final int KEY_STEP = 66;
    private static final int KEY_SPRITE_V = 236;
    private static final int BRAND_Y = 19;
    private static final int LEFT = SCREEN_X + 5;
    private static final int RIGHT = SCREEN_X + SCREEN_W - 5;
    private static final int CONTENT_W = RIGHT - LEFT;
    private static final int BAR_Y = SCREEN_Y + 5;
    private static final int TOP = BAR_Y + 13;
    private static final int BUTTON_Y = SCREEN_Y + SCREEN_H - 14;
    private static final int ROW = 11;
    private static final int ZONE_ROWS = 10;
    /** Locker rows the picker shows, leaving room for the reason line and the buttons. */
    private static final int PICKER_ROWS = 9;
    private static final int LIST_Y = TOP + 12;
    private static final int CELL = 19;
    private static final float COUNT_SCALE = 0.75F;
    private static final int GRID_COLUMNS = 9;
    /** Stock rows in STOCK, leaving room for the basket below. */
    private static final int STOCK_ROWS = 4;
    private static final int BASKET_LABEL_Y = LIST_Y + STOCK_ROWS * CELL + 3;
    private static final int BASKET_Y = BASKET_LABEL_Y + 10;
    /** How long the basket full note stays. */
    private static final long NOTE_MS = 2500;
    private static final int SWATCH = 11;
    private static final int TAG_ROW = 10;
    private static final int TAG_COLUMN = CONTENT_W / 2;
    private static final int LCD = 0xFF0F1F12;
    private static final int LCD_TEXT = 0xFF8DFF9A;
    private static final int LCD_DIM = 0xFF3F8A4A;
    private static final int LCD_HOVER = 0xFF173A1D;
    private static final int LCD_LINE = 0xFF1F4A26;
    private static final int BRAND = 0xFFAEB4BD;
    private static final int KEY_TEXT = 0xFFDFE3E8;
    private static final int KEY_LIT_TEXT = 0xFF1D1404;
    private static final int GLOW_RGB = 0x8DFF9A;
    private static final double GLOW_PERIOD_MS = 4200;
    private static final HandheldMode[] SOFTKEYS = {HandheldMode.ZONE, HandheldMode.INFO, HandheldMode.STOCK};
    private static List<ItemPicker.Entry> itemEntries;

    private enum View {
        ZONES,
        FORM,
        INFO,
        STOCK,
        PICKER
    }

    /** An item grid on the screen: its top, how many rows show, and its items with their counts. */
    private record Grid(int y, int rows, List<ItemStack> items, List<Long> counts) {

        /** How many rows the grid can scroll down. */
        int scrollLines() {
            return (items.size() + GRID_COLUMNS - 1) / GRID_COLUMNS - rows;
        }
    }

    /** A text button on the CRT: inverted while hovered, dim while it cannot be used, gone while not shown. */
    private record CrtButton(int x, int y, Supplier<Component> label, BooleanSupplier shown, BooleanSupplier active, Runnable press) {

        boolean usable() {
            return shown.getAsBoolean() && active.getAsBoolean();
        }
    }

    private View view;
    private final List<CrtButton> buttons = new ArrayList<>();
    private EditBox search;
    private EditBox name;
    private final ItemPicker picker;
    private int scroll;
    /** The zone the form edits, 0 for a new one. */
    private int editing;
    private int colour;
    private final List<ItemStack> filterItems = new ArrayList<>();
    private final List<String> filterTags = new ArrayList<>();
    /** The zone whose delete waits for a second click, 0 for none. */
    private int confirmDelete;
    private final BasketAmountPopup amountPopup;
    /** Until when the basket full note shows, in milliseconds. */
    private long basketFullUntil;
    /** The locker picker REQUEST opened, null while it is closed. */
    private LockerPicker lockerPicker;

    public HandheldScreen(HandheldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, HEIGHT);
        view = viewOf(menu.handheld().getOrDefault(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE));
        picker = new ItemPicker(Component.translatable(KEY + "picker"), this::addToFilter);
        picker.setTagSearch(true);
        amountPopup = new BasketAmountPopup(this::confirmAmount,
                item -> ClientPacketDistributor.sendToServer(HandheldBasketPayload.remove(item)));
    }

    private static View viewOf(HandheldMode mode) {
        return switch (mode) {
            case ZONE -> View.ZONES;
            case INFO -> View.INFO;
            case STOCK -> View.STOCK;
        };
    }

    private HandheldMode modeOf(View view) {
        return switch (view) {
            case ZONES, FORM -> HandheldMode.ZONE;
            case INFO -> HandheldMode.INFO;
            case STOCK, PICKER -> HandheldMode.STOCK;
        };
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = -1000;
        inventoryLabelY = -1000;
        search = addRenderableWidget(crtBox(LEFT, TOP, CONTENT_W, 50, "search_hint"));
        search.setResponder(text -> scroll = 0);
        int nameX = LEFT + font.width(Component.translatable(KEY + "name")) + 4;
        name = addRenderableWidget(crtBox(nameX, TOP, RIGHT - nameX, ZoneList.MAX_NAME, "name_hint"));
        picker.setEntries(itemEntries());
        picker.setArea(leftPos, topPos, imageWidth, imageHeight);
        amountPopup.setArea(leftPos, topPos, imageWidth, imageHeight);
        amountPopup.close();
        show(view);
    }

    private EditBox crtBox(int x, int y, int width, int maxLength, String hint) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 10, Component.translatable(KEY + hint));
        box.setMaxLength(maxLength);
        box.setBordered(false);
        box.setTextShadow(false);
        box.setTextColor(LCD_TEXT);
        box.setHint(Component.translatable(KEY + hint).withColor(LCD_DIM));
        return box;
    }

    /** Every registered item but air, for the filter picker; built once per client session. */
    private static List<ItemPicker.Entry> itemEntries() {
        if (itemEntries == null) {
            List<ItemPicker.Entry> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == Items.AIR) {
                    continue;
                }
                ItemStack stack = new ItemStack(item);
                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                list.add(new ItemPicker.Entry(id, stack, PickerEntries.nameOr(stack::getHoverName, id)));
            }
            itemEntries = List.copyOf(list);
        }
        return itemEntries;
    }

    private void show(View next) {
        view = next;
        scroll = 0;
        if (view != View.PICKER) {
            lockerPicker = null;
        }
        confirmDelete = ZoneList.NONE;
        search.visible = view == View.ZONES || view == View.STOCK && menu.bound();
        search.setValue("");
        name.visible = view == View.FORM;
        buttons.clear();
        switch (view) {
            case ZONES -> {
                addButton(LEFT, "new", () -> true, () -> openForm(ZoneList.NONE));
                addButton(LEFT + 40, "edit", () -> activeTeamZone() != null, this::editActive);
                buttons.add(new CrtButton(LEFT + 80, BUTTON_Y, this::deleteLabel, () -> true, () -> activeTeamZone() != null, this::deleteActive));
            }
            case FORM -> {
                addButton(LEFT, "save", () -> problem().isEmpty(), this::saveForm);
                addButton(LEFT + 44, "cancel", () -> true, () -> show(View.ZONES));
            }
            case INFO -> {
                addClearButton();
                int relocateX = LEFT + font.width(Component.translatable(KEY + "basket_clear")) + 10;
                addButton(relocateX, "relocate", this::basketShown, () -> !basket().isEmpty() && menu.bound(),
                        () -> ClientPacketDistributor.sendToServer(HandheldRelocatePayload.INSTANCE));
            }
            case STOCK -> {
                addClearButton();
                int requestX = LEFT + font.width(Component.translatable(KEY + "basket_clear")) + 10;
                addButton(requestX, "request", this::basketShown, () -> !basket().isEmpty(), this::openPicker);
            }
            case PICKER -> {
                addButton(LEFT, "cancel", () -> true, () -> show(View.STOCK));
                addButton(LEFT + font.width(Component.translatable(KEY + "cancel")) + 10, "order",
                        () -> lockerPicker != null && lockerPicker.chosen().isPresent(), this::send);
            }
        }
        setFocused(view == View.FORM ? name : search.visible ? search : null);
    }

    private void addButton(int x, String key, BooleanSupplier active, Runnable press) {
        addButton(x, key, () -> true, active, press);
    }

    private void addButton(int x, String key, BooleanSupplier shown, BooleanSupplier active, Runnable press) {
        Component label = Component.translatable(KEY + key);
        buttons.add(new CrtButton(x, BUTTON_Y, () -> label, shown, active, press));
    }

    /** Clear for the basket of the screen, shown with the basket. */
    private void addClearButton() {
        addButton(LEFT, "basket_clear", this::basketShown, () -> !basket().isEmpty(),
                () -> ClientPacketDistributor.sendToServer(HandheldBasketPayload.clear()));
    }

    /** Delete, or the second click's question with the number of loaded shelves in the zone. */
    private Component deleteLabel() {
        ZoneList.Zone<ZoneFilter> zone = activeTeamZone();
        return zone != null && confirmDelete == zone.id()
                ? Component.translatable(KEY + "delete_confirm", menu.shelfCount(zone.id()))
                : Component.translatable(KEY + "delete");
    }

    /** Switches the handheld to a mode, as sneak scrolling does, and shows its view. */
    private void softkey(HandheldMode mode) {
        ClientHandheld.clicked();
        if (modeOf(view) == mode && view != View.FORM) {
            return;
        }
        ClientPacketDistributor.sendToServer(HandheldModePayload.to(mode));
        show(viewOf(mode));
    }

    /** The team's zones matching the search, after the "No zone" entry. */
    private List<ZoneList.Zone<ZoneFilter>> zoneRows() {
        return ClientZones.list().matching(menu.team(), search.getValue());
    }

    private ZoneList.Zone<ZoneFilter> activeTeamZone() {
        Integer active = menu.activeZone();
        ZoneList.Zone<ZoneFilter> zone = active == null ? null : ClientZones.list().get(active);
        return zone != null && zone.team().equals(menu.team()) ? zone : null;
    }

    private void openForm(int id) {
        editing = id;
        ZoneList.Zone<ZoneFilter> zone = ClientZones.list().get(id);
        show(View.FORM);
        name.setValue(zone == null ? "" : zone.name());
        colour = zone == null ? 0 : zone.colour();
        filterItems.clear();
        filterTags.clear();
        if (zone != null) {
            zone.filter().items().forEach(stack -> filterItems.add(stack.copyWithCount(1)));
            filterTags.addAll(zone.filter().tags());
        }
    }

    private void editActive() {
        ZoneList.Zone<ZoneFilter> zone = activeTeamZone();
        if (zone != null) {
            openForm(zone.id());
        }
    }

    private void deleteActive() {
        ZoneList.Zone<ZoneFilter> zone = activeTeamZone();
        if (zone == null) {
            return;
        }
        if (menu.shelfCount(zone.id()) > 0 && confirmDelete != zone.id()) {
            confirmDelete = zone.id();
            return;
        }
        confirmDelete = ZoneList.NONE;
        ClientPacketDistributor.sendToServer(HandheldZoneActionPayload.remove(zone.id()));
    }

    private Optional<ZoneList.NameProblem> problem() {
        return ClientZones.list().nameProblem(menu.team(), name.getValue(), editing);
    }

    private void saveForm() {
        if (problem().isPresent()) {
            return;
        }
        ZoneFilter filter = new ZoneFilter(List.copyOf(filterItems), List.copyOf(filterTags));
        ClientPacketDistributor.sendToServer(new HandheldZonePayload(editing, name.getValue(), colour, filter));
        show(View.ZONES);
    }

    /** A pick from the item picker: a {@code #} entry adds a tag, any other entry its item; nine of each at most. */
    private void addToFilter(ItemPicker.Entry entry) {
        if (TagSearch.isTagQuery(entry.id())) {
            String tag = entry.id().substring(TagSearch.PREFIX.length());
            if (!filterTags.contains(tag) && filterTags.size() < ZoneFilter.SLOTS) {
                filterTags.add(tag);
            }
        } else if (!entry.icon().isEmpty() && filterItems.size() < ZoneFilter.SLOTS
                && filterItems.stream().noneMatch(stack -> ItemStack.isSameItemSameComponents(stack, entry.icon()))) {
            filterItems.add(entry.icon().copyWithCount(1));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (view == View.STOCK) {
            search.visible = menu.bound();
        }
        boolean popupOpen = picker.isOpen() || amountPopup.isOpen();
        int mx = popupOpen ? -1 : mouseX;
        int my = popupOpen ? -1 : mouseY;
        super.extractRenderState(graphics, mx, my, partialTick);
        drawCrtOverlay(graphics);
        if (amountPopup.isOpen()) {
            amountPopup.render(graphics, font, mouseX, mouseY, partialTick);
            return;
        }
        if (picker.isOpen()) {
            picker.renderPopup(graphics, font, mouseX, mouseY, partialTick);
            picker.renderTooltip(graphics, font, mouseX, mouseY);
            return;
        }
        hoveredItem(mouseX, mouseY).ifPresent(stack -> graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        graphics.text(font, Component.translatable(KEY + "brand"), leftPos + SCREEN_X + 2, topPos + BRAND_Y, BRAND, false);
        drawSoftkeys(graphics);
        graphics.fill(x(SCREEN_X), y(SCREEN_Y), x(SCREEN_X + SCREEN_W), y(SCREEN_Y + SCREEN_H), LCD);
        switch (view) {
            case ZONES -> drawZones(graphics, mouseX, mouseY);
            case FORM -> drawForm(graphics, mouseX, mouseY);
            case INFO -> drawInfo(graphics, mouseX, mouseY);
            case STOCK -> drawStock(graphics, mouseX, mouseY);
            case PICKER -> drawPicker(graphics, mouseX, mouseY);
        }
        for (CrtButton button : buttons) {
            if (button.shown().getAsBoolean()) {
                drawButton(graphics, button, mouseX, mouseY);
            }
        }
    }

    private int x(int guiX) {
        return leftPos + guiX;
    }

    private int y(int guiY) {
        return topPos + guiY;
    }

    private void drawSoftkeys(GuiGraphicsExtractor graphics) {
        HandheldMode current = modeOf(view);
        for (int i = 0; i < SOFTKEYS.length; i++) {
            boolean lit = SOFTKEYS[i] == current;
            int kx = x(SCREEN_X + i * KEY_STEP);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, kx, y(KEY_Y), lit ? KEY_W : 0, KEY_SPRITE_V, KEY_W, KEY_H, 256, 256);
            String label = SOFTKEYS[i].getSerializedName().toUpperCase(Locale.ROOT);
            graphics.text(font, label, kx + (KEY_W - font.width(label)) / 2, y(KEY_Y) + 5, lit ? KEY_LIT_TEXT : KEY_TEXT, false);
        }
    }

    /** Glow, scanlines and vignette over everything on the screen. */
    private void drawCrtOverlay(GuiGraphicsExtractor graphics) {
        double phase = Util.getMillis() % (long) GLOW_PERIOD_MS / GLOW_PERIOD_MS * Math.PI * 2;
        int glowAlpha = (int) (11 + 4 * Math.sin(phase));
        graphics.fill(x(SCREEN_X), y(SCREEN_Y), x(SCREEN_X + SCREEN_W), y(SCREEN_Y + SCREEN_H), glowAlpha << 24 | GLOW_RGB);
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCANLINES, x(SCREEN_X), y(SCREEN_Y), 0, 0, SCREEN_W, SCREEN_H, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, x(SCREEN_X), y(SCREEN_Y), 0, 0, SCREEN_W, SCREEN_H, SCREEN_W, SCREEN_H);
    }

    /** The top bar: a title on the left, an optional text on the right, and a dim line below. */
    private void drawBar(GuiGraphicsExtractor graphics, Component title, String right) {
        graphics.text(font, title, x(LEFT), y(BAR_Y), LCD_TEXT, false);
        if (!right.isEmpty()) {
            String cut = font.plainSubstrByWidth(right, CONTENT_W - font.width(title) - 8);
            graphics.text(font, cut, x(RIGHT) - font.width(cut), y(BAR_Y), LCD_DIM, false);
        }
        graphics.fill(x(LEFT), y(BAR_Y + 10), x(RIGHT), y(BAR_Y + 11), LCD_LINE);
    }

    private void drawButton(GuiGraphicsExtractor graphics, CrtButton button, int mouseX, int mouseY) {
        boolean active = button.active().getAsBoolean();
        Component label = button.label().get();
        int width = font.width(label) + 6;
        int bx = x(button.x());
        int by = y(button.y());
        boolean hovered = active && inside(mouseX, mouseY, bx, by - 1, width, 11);
        graphics.outline(bx, by - 1, width, 11, active ? LCD_DIM : LCD_LINE);
        if (hovered) {
            graphics.fill(bx + 1, by, bx + width - 1, by + 9, LCD_TEXT);
        }
        graphics.text(font, label, bx + 3, by + 1, hovered ? LCD : active ? LCD_TEXT : LCD_DIM, false);
    }

    private void drawZones(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawBar(graphics, Component.translatable(KEY + "view.zones"), "");
        List<ZoneList.Zone<ZoneFilter>> rows = zoneRows();
        Integer active = menu.activeZone();
        for (int row = 0; row < ZONE_ROWS; row++) {
            int index = scroll + row;
            if (index > rows.size()) {
                break;
            }
            int top = y(LIST_Y + row * ROW);
            ZoneList.Zone<ZoneFilter> zone = index == 0 ? null : rows.get(index - 1);
            int id = zone == null ? ZoneList.NONE : zone.id();
            boolean isActive = active != null && active == id;
            boolean hovered = inside(mouseX, mouseY, x(LEFT), top, CONTENT_W, ROW);
            if (isActive) {
                graphics.fill(x(LEFT), top, x(RIGHT), top + ROW, LCD_TEXT);
            } else if (hovered) {
                graphics.fill(x(LEFT), top, x(RIGHT), top + ROW, LCD_HOVER);
            }
            int text = isActive ? LCD : LCD_TEXT;
            int dim = isActive ? LCD : LCD_DIM;
            if (zone == null) {
                graphics.text(font, Component.translatable(KEY + "no_zone"), x(LEFT) + 11, top + 2, text, false);
                continue;
            }
            graphics.fill(x(LEFT) + 1, top + 2, x(LEFT) + 8, top + 9, ChannelColours.argb(zone.colour()));
            String count = Integer.toString(menu.shelfCount(zone.id()));
            int countX = x(RIGHT) - 1 - font.width(count);
            String zoneName = font.plainSubstrByWidth(zone.name(), 74);
            graphics.text(font, zoneName, x(LEFT) + 11, top + 2, text, false);
            int summaryX = x(LEFT) + 11 + font.width(zoneName) + 6;
            String summary = font.plainSubstrByWidth(filterSummary(zone.filter()), Math.max(0, countX - 4 - summaryX));
            graphics.text(font, summary, summaryX, top + 2, dim, false);
            graphics.text(font, count, countX, top + 2, dim, false);
        }
    }

    /** The filter in a few words: its tags and item names, or "anything". */
    static String filterSummary(ZoneFilter filter) {
        if (filter.isEmpty()) {
            return Component.translatable(KEY + "takes_anything").getString();
        }
        List<String> parts = new ArrayList<>(filter.tags().stream().map(tag -> TagSearch.PREFIX + tag).toList());
        parts.addAll(filter.itemNames());
        return String.join(", ", parts);
    }

    private static final int FORM_PALETTE_Y = TOP + 12;
    private static final int FORM_ITEMS_LABEL_Y = FORM_PALETTE_Y + 13;
    private static final int FORM_ITEMS_Y = FORM_ITEMS_LABEL_Y + 10;
    private static final int FORM_TAGS_LABEL_Y = FORM_ITEMS_Y + CELL + 2;
    private static final int FORM_TAGS_Y = FORM_TAGS_LABEL_Y + 10;
    private static final int FORM_TAG_ROWS = 5;
    private static final int FORM_PROBLEM_Y = FORM_TAGS_Y + FORM_TAG_ROWS * TAG_ROW + 1;

    private void drawForm(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawBar(graphics, Component.translatable(KEY + (editing == ZoneList.NONE ? "view.new_zone" : "view.edit_zone")), "");
        graphics.text(font, Component.translatable(KEY + "name"), x(LEFT), y(TOP + 1), LCD_DIM, false);
        graphics.fill(name.getX(), name.getY() + 9, name.getX() + name.getWidth(), name.getY() + 10, LCD_LINE);
        for (int i = 0; i < ZoneList.COLOURS; i++) {
            int sx = x(LEFT + i * SWATCH);
            int sy = y(FORM_PALETTE_Y);
            if (i == colour) {
                graphics.outline(sx - 1, sy - 1, SWATCH, SWATCH, LCD_TEXT);
            }
            graphics.fill(sx, sy, sx + SWATCH - 2, sy + SWATCH - 2, ChannelColours.argb(i));
        }
        graphics.text(font, Component.translatable(KEY + "items", filterItems.size(), ZoneFilter.SLOTS), x(LEFT), y(FORM_ITEMS_LABEL_Y), LCD_DIM, false);
        for (int i = 0; i < ZoneFilter.SLOTS; i++) {
            int cx = x(LEFT + i * CELL);
            int cy = y(FORM_ITEMS_Y);
            graphics.outline(cx, cy, CELL - 1, CELL - 1, LCD_LINE);
            if (i < filterItems.size()) {
                graphics.fakeItem(filterItems.get(i), cx + 1, cy + 1);
            }
        }
        graphics.text(font, Component.translatable(KEY + "tags", filterTags.size(), ZoneFilter.SLOTS), x(LEFT), y(FORM_TAGS_LABEL_Y), LCD_DIM, false);
        for (int i = 0; i < filterTags.size(); i++) {
            int tx = x(LEFT + i / FORM_TAG_ROWS * TAG_COLUMN);
            int ty = y(FORM_TAGS_Y + i % FORM_TAG_ROWS * TAG_ROW);
            boolean hovered = inside(mouseX, mouseY, tx, ty, TAG_COLUMN - 2, TAG_ROW);
            if (hovered) {
                graphics.fill(tx, ty, tx + TAG_COLUMN - 2, ty + TAG_ROW, LCD_HOVER);
            }
            String tag = font.plainSubstrByWidth(TagSearch.PREFIX + filterTags.get(i), TAG_COLUMN - 4);
            graphics.text(font, tag, tx + 1, ty + 1, LCD_TEXT, false);
        }
        problem().ifPresent(problem -> graphics.text(font,
                Component.translatable(KEY + "problem." + problem.name().toLowerCase(Locale.ROOT)), x(LEFT), y(FORM_PROBLEM_Y), LCD_TEXT, false));
        drawAddButton(graphics, mouseX, mouseY);
    }

    private Component addLabel() {
        return Component.translatable(KEY + "add");
    }

    private boolean canAdd() {
        return filterItems.size() < ZoneFilter.SLOTS || filterTags.size() < ZoneFilter.SLOTS;
    }

    private int addX() {
        return RIGHT - font.width(addLabel()) - 6;
    }

    private void drawAddButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int bx = x(addX());
        int by = y(FORM_ITEMS_LABEL_Y);
        int width = font.width(addLabel()) + 6;
        boolean active = canAdd();
        boolean hovered = active && inside(mouseX, mouseY, bx, by - 1, width, 11);
        graphics.outline(bx, by - 1, width, 11, active ? LCD_DIM : LCD_LINE);
        if (hovered) {
            graphics.fill(bx + 1, by, bx + width - 1, by + 9, LCD_TEXT);
        }
        graphics.text(font, addLabel(), bx + 3, by + 1, hovered ? LCD : active ? LCD_TEXT : LCD_DIM, false);
    }

    private void drawStock(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HandheldBinding binding = menu.handheld().get(ServusShuttlesComponents.HANDHELD_BINDING.get());
        drawBar(graphics, Component.translatable(KEY + "view.stock"), binding == null ? "" : binding.name());
        if (!menu.bound()) {
            graphics.centeredText(font, Component.translatable(KEY + "not_bound"), x(SCREEN_X + SCREEN_W / 2), y(SCREEN_Y + 60), LCD_TEXT);
            graphics.centeredText(font, Component.translatable(KEY + "bind_hint"), x(SCREEN_X + SCREEN_W / 2), y(SCREEN_Y + 74), LCD_DIM);
            return;
        }
        graphics.fill(x(LEFT), y(TOP + 9), x(RIGHT), y(TOP + 10), LCD_LINE);
        grid().ifPresent(grid -> {
            if (grid.items().isEmpty()) {
                graphics.text(font, Component.translatable(KEY + "stock_empty"), x(LEFT), y(LIST_Y + 2), LCD_DIM, false);
            }
            drawGrid(graphics, grid, mouseX, mouseY);
        });
        drawBasket(graphics, mouseX, mouseY);
    }

    /** REQUEST: asks the server for the lockers and shows the picker while it waits. */
    private void openPicker() {
        ClientHandheld.takeLockers();
        ClientPacketDistributor.sendToServer(HandheldPickerPayload.INSTANCE);
        show(View.PICKER);
        lockerPicker = new LockerPicker();
    }

    /** ORDER: orders the basket to the chosen locker; the server empties the basket. */
    private void send() {
        if (lockerPicker == null) {
            return;
        }
        lockerPicker.chosen().ifPresent(dock -> {
            ClientPacketDistributor.sendToServer(new HandheldRequestPayload(dock));
            ClientHandheld.forgetParcel();
            onClose();
        });
    }

    /** The locker picker: the lockers nearest first, with distance and free boxes, greyed when they cannot take the order. */
    private void drawPicker(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawBar(graphics, Component.translatable(KEY + "view.picker"), "");
        if (lockerPicker == null) {
            return;
        }
        List<LockerPicker.Entry> entries = lockerPicker.entries();
        for (int row = 0; row < Math.min(PICKER_ROWS, entries.size() - scroll); row++) {
            int index = scroll + row;
            LockerPicker.Entry entry = entries.get(index);
            int top = y(TOP + 1 + row * ROW);
            boolean selected = index == lockerPicker.selected();
            boolean fits = entry.row().fits();
            if (selected) {
                graphics.fill(x(LEFT), top - 1, x(RIGHT), top + ROW - 1, LCD_TEXT);
            } else if (fits && inside(mouseX, mouseY, x(LEFT), top - 1, CONTENT_W, ROW)) {
                graphics.fill(x(LEFT), top - 1, x(RIGHT), top + ROW - 1, LCD_HOVER);
            }
            int colour = selected ? LCD : fits ? LCD_TEXT : LCD_LINE;
            List<Integer> free = entry.row().free();
            String right = Component.translatable(KEY + "picker.row", entry.distance(), free.get(0), free.get(1), free.get(2)).getString();
            graphics.text(font, right, x(RIGHT) - 1 - font.width(right), top + 1, colour, false);
            String label = font.plainSubstrByWidth(entry.row().label().getString(), CONTENT_W - font.width(right) - 8);
            graphics.text(font, label, x(LEFT) + 2, top + 1, colour, false);
        }
        String reason = lockerPicker.reason();
        if (!reason.isEmpty()) {
            graphics.text(font, font.plainSubstrByWidth(Component.translatable(KEY + reason).getString(), CONTENT_W), x(LEFT),
                    y(BUTTON_Y - ROW - 2), LCD_TEXT, false);
        }
    }

    /** A click on a picker row selects that locker when it takes the order. */
    private boolean clickPicker(double mouseX, double mouseY) {
        if (lockerPicker == null || !inside(mouseX, mouseY, x(LEFT), y(TOP), CONTENT_W, PICKER_ROWS * ROW)) {
            return false;
        }
        lockerPicker.select(scroll + (int) ((mouseY - y(TOP)) / ROW));
        return true;
    }

    /** The readout of the last scanned shelf: zone and slots, the zone's filter, then its items and the shelf basket. */
    private void drawInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        drawBar(graphics, Component.translatable(KEY + "view.info"), info == null || info.code().isEmpty() ? "" : info.code());
        if (info == null) {
            graphics.text(font, Component.translatable(KEY + "info.scan_hint"), x(LEFT), y(TOP + 1), LCD_DIM, false);
            return;
        }
        int line = y(TOP + 1);
        String amount = (info.fluid() ? Component.translatable(KEY + "info.fluid", info.used(), info.total())
                : Component.translatable(KEY + "info.slots", info.used(), info.total())).getString();
        graphics.text(font, amount, x(RIGHT) - font.width(amount), line, LCD_TEXT, false);
        int nameWidth = CONTENT_W - font.width(amount) - 6;
        ZoneDefinition zone = ClientZones.definition(info.zoneId());
        if (zone == null) {
            graphics.text(font, font.plainSubstrByWidth(Component.translatable(KEY + "no_zone").getString(), nameWidth), x(LEFT), line, LCD_TEXT, false);
        } else {
            graphics.fill(x(LEFT) + 1, line, x(LEFT) + 8, line + 7, ChannelColours.argb(zone.colour()));
            graphics.text(font, font.plainSubstrByWidth(zone.name(), nameWidth - 11), x(LEFT) + 11, line, LCD_TEXT, false);
            graphics.text(font, font.plainSubstrByWidth(filterSummary(zone.filter()), CONTENT_W), x(LEFT), line + ROW, LCD_DIM, false);
        }
        int top = y(infoGridY(info));
        graphics.fill(x(LEFT), top - 3, x(RIGHT), top - 2, LCD_LINE);
        if (info.top().isEmpty()) {
            graphics.text(font, Component.translatable(KEY + "info.empty"), x(LEFT), top + 4, LCD_DIM, false);
        }
        if (info.fluid()) {
            for (int i = 0; i < info.top().size(); i++) {
                int rowY = top + i * CELL;
                ItemStack stack = info.top().get(i);
                graphics.fakeItem(stack, x(LEFT), rowY);
                String count = Component.translatable(KEY + "info.mb", info.counts().get(i)).getString();
                String itemName = font.plainSubstrByWidth(stack.getHoverName().getString(), CONTENT_W - 24 - font.width(count));
                graphics.text(font, itemName, x(LEFT) + 20, rowY + 5, LCD_TEXT, false);
                graphics.text(font, count, x(RIGHT) - font.width(count), rowY + 5, LCD_TEXT, false);
            }
            return;
        }
        grid().ifPresent(grid -> drawGrid(graphics, grid, mouseX, mouseY));
        drawBasket(graphics, mouseX, mouseY);
    }

    /** Where the INFO item grid starts: below the zone line, and the filter line when zoned. */
    private static int infoGridY(HandheldInfoPayload info) {
        boolean zoned = ClientZones.definition(info.zoneId()) != null;
        return TOP + 1 + ROW + (zoned ? ROW : 0) + 4;
    }

    /** The scanned item shelf INFO shows, or null before a scan and for a Fluid Rack shelf. */
    private static HandheldInfoPayload shownShelf() {
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        return info == null || info.fluid() ? null : info;
    }

    /** The item grid of the screen: the scanned shelf's items in INFO, the warehouse stock matching the search in STOCK. */
    private Optional<Grid> grid() {
        if (view == View.STOCK && menu.bound()) {
            List<ItemStack> items = new ArrayList<>();
            List<Long> counts = new ArrayList<>();
            for (int index : stockRows()) {
                items.add(menu.stock().get(index));
                counts.add(menu.stockCounts().get(index));
            }
            return Optional.of(new Grid(LIST_Y, STOCK_ROWS, items, counts));
        }
        HandheldInfoPayload info = shownShelf();
        if (view == View.INFO && info != null) {
            int top = infoGridY(info);
            return Optional.of(new Grid(top, (BASKET_LABEL_Y - 3 - top) / CELL, info.top(), info.counts()));
        }
        return Optional.empty();
    }

    private void drawGrid(GuiGraphicsExtractor graphics, Grid grid, int mouseX, int mouseY) {
        for (int cell = 0; cell < GRID_COLUMNS * grid.rows(); cell++) {
            int index = scroll * GRID_COLUMNS + cell;
            if (index >= grid.items().size()) {
                break;
            }
            int sx = x(LEFT + (cell % GRID_COLUMNS) * CELL);
            int sy = y(grid.y() + (cell / GRID_COLUMNS) * CELL);
            boolean hovered = inside(mouseX, mouseY, sx, sy, CELL - 1, CELL - 1);
            if (hovered) {
                graphics.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, LCD_HOVER);
            }
            graphics.outline(sx, sy, CELL - 1, CELL - 1, hovered ? LCD_TEXT : LCD_LINE);
            graphics.fakeItem(grid.items().get(index), sx + 1, sy + 1);
            drawCount(graphics, CompactCount.slot(grid.counts().get(index)), sx + CELL - 1, sy + CELL - 1);
        }
    }

    /** The index into the grid's items of the cell under the mouse, or -1. */
    private int hoveredCell(Grid grid, double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - x(LEFT)) / CELL);
        int row = (int) Math.floor((mouseY - y(grid.y())) / CELL);
        if (column < 0 || column >= GRID_COLUMNS || row < 0 || row >= grid.rows()) {
            return -1;
        }
        int index = (scroll + row) * GRID_COLUMNS + column;
        return index < grid.items().size() ? index : -1;
    }

    /** The item of the grid cell under the mouse. */
    private Optional<ItemStack> hoveredGridItem(double mouseX, double mouseY) {
        return grid().flatMap(grid -> {
            int index = hoveredCell(grid, mouseX, mouseY);
            return index < 0 ? Optional.empty() : Optional.of(grid.items().get(index));
        });
    }

    /** The basket under the list: a label with the entry count and the click hint, then one cell per entry. */
    private void drawBasket(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Basket<ItemKey> basket = basket();
        graphics.fill(x(LEFT), y(BASKET_LABEL_Y - 2), x(RIGHT), y(BASKET_LABEL_Y - 1), LCD_LINE);
        Component label = Component.translatable(KEY + (view == View.INFO ? "shelf_basket" : "basket"), basket.size(), Basket.MAX_ENTRIES);
        graphics.text(font, label, x(LEFT), y(BASKET_LABEL_Y), LCD_DIM, false);
        boolean full = Util.getMillis() < basketFullUntil;
        Component hint = full ? Component.translatable(KEY + "basket_full")
                : Component.translatable(KEY + (hoveredBasket(mouseX, mouseY) >= 0 ? "basket_hint_entry" : "basket_hint_stock"));
        String cut = font.plainSubstrByWidth(hint.getString(), CONTENT_W - font.width(label) - 6);
        graphics.text(font, cut, x(RIGHT) - font.width(cut), y(BASKET_LABEL_Y), full ? LCD_TEXT : LCD_DIM, false);
        for (int i = 0; i < Basket.MAX_ENTRIES; i++) {
            int sx = x(LEFT + i * CELL);
            int sy = y(BASKET_Y);
            boolean hovered = i < basket.size() && inside(mouseX, mouseY, sx, sy, CELL - 1, CELL - 1);
            if (hovered) {
                graphics.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, LCD_HOVER);
            }
            graphics.outline(sx, sy, CELL - 1, CELL - 1, hovered ? LCD_TEXT : LCD_LINE);
            if (i < basket.size()) {
                Basket.Entry<ItemKey> entry = basket.entries().get(i);
                graphics.fakeItem(entry.key().stack(1), sx + 1, sy + 1);
                drawCount(graphics, CompactCount.slot(entry.amount()), sx + CELL - 1, sy + CELL - 1);
            }
        }
    }

    /** The basket of the screen: the shelf basket in INFO, the warehouse basket in STOCK. */
    private Basket<ItemKey> basket() {
        return HandheldRelocation.basket(menu.handheld(), view == View.INFO);
    }

    /** Whether the screen shows a basket: INFO on a scanned item shelf, or STOCK of a bound handheld. */
    private boolean basketShown() {
        return view == View.INFO ? shownShelf() != null : view == View.STOCK && menu.bound();
    }

    /** The index of the basket entry under the mouse, or -1. */
    private int hoveredBasket(double mouseX, double mouseY) {
        if (!basketShown() || mouseY < y(BASKET_Y) || mouseY >= y(BASKET_Y + CELL - 1) || mouseX < x(LEFT)) {
            return -1;
        }
        int index = (int) ((mouseX - x(LEFT)) / CELL);
        return index < basket().size() ? index : -1;
    }

    /** A list item clicked: left opens the amount popup (shift adds a stack at once), right lights up the item's shelves. */
    private void clickListItem(ItemStack item, int button, boolean shift) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientPacketDistributor.sendToServer(new HandheldFindPayload(item));
            onClose();
            return;
        }
        if (!basket().canAdd(ItemKey.of(item))) {
            basketFullUntil = Util.getMillis() + NOTE_MS;
            return;
        }
        int room = held(item) - basket().amount(ItemKey.of(item));
        if (room <= 0) {
            return;
        }
        if (shift) {
            ClientPacketDistributor.sendToServer(HandheldBasketPayload.add(item, Math.min(item.getMaxStackSize(), room)));
        } else {
            amountPopup.open(font, item, Math.min(item.getMaxStackSize(), room), room, false);
        }
    }

    /** A basket cell clicked: left edits its amount, right removes it. */
    private void clickBasket(int index, int button) {
        Basket.Entry<ItemKey> entry = basket().entries().get(index);
        ItemStack item = entry.key().stack(1);
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            ClientPacketDistributor.sendToServer(HandheldBasketPayload.remove(item));
        } else {
            amountPopup.open(font, item, entry.amount(), Math.max(entry.amount(), held(item)), true);
        }
    }

    /** How many of the item the screen's list shows, at most {@link AmountSteps#MAX}; 0 when the list does not show it. */
    private int held(ItemStack item) {
        ItemKey key = ItemKey.of(item);
        List<ItemStack> items = view == View.STOCK ? menu.stock() : shownShelf() == null ? List.of() : shownShelf().top();
        List<Long> counts = view == View.STOCK ? menu.stockCounts() : shownShelf() == null ? List.of() : shownShelf().counts();
        for (int i = 0; i < items.size(); i++) {
            if (ItemKey.of(items.get(i)).equals(key)) {
                return (int) Math.min(AmountSteps.MAX, counts.get(i));
            }
        }
        return 0;
    }

    /** Add raises an entry already in the basket; Save on an entry sets its amount. */
    private void confirmAmount(ItemStack item, int amount, boolean editing) {
        ClientPacketDistributor.sendToServer(editing ? HandheldBasketPayload.set(item, amount) : HandheldBasketPayload.add(item, amount));
    }

    /** A stack count in small CRT text, its bottom right corner at the given point. */
    private void drawCount(GuiGraphicsExtractor graphics, String count, int right, int bottom) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(right - font.width(count) * COUNT_SCALE, bottom - (font.lineHeight - 1) * COUNT_SCALE);
        graphics.pose().scale(COUNT_SCALE, COUNT_SCALE);
        graphics.text(font, count, 0, 0, LCD_TEXT, true);
        graphics.pose().popMatrix();
    }

    /** Indices into the menu's stock that match the search, largest first. */
    private List<Integer> stockRows() {
        String query = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < menu.stock().size(); i++) {
            if (query.isEmpty() || menu.stock().get(i).getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                rows.add(i);
            }
        }
        return rows;
    }

    private int hoveredFilterItem(double mouseX, double mouseY) {
        if (view != View.FORM || mouseY < y(FORM_ITEMS_Y) || mouseY >= y(FORM_ITEMS_Y + CELL - 1) || mouseX < x(LEFT)) {
            return -1;
        }
        int index = (int) ((mouseX - x(LEFT)) / CELL);
        return index < filterItems.size() ? index : -1;
    }

    private int hoveredFilterTag(double mouseX, double mouseY) {
        if (view != View.FORM || mouseX < x(LEFT) || mouseY < y(FORM_TAGS_Y)) {
            return -1;
        }
        int column = (int) ((mouseX - x(LEFT)) / TAG_COLUMN);
        int row = (int) ((mouseY - y(FORM_TAGS_Y)) / TAG_ROW);
        int index = column * FORM_TAG_ROWS + row;
        return column < 2 && row < FORM_TAG_ROWS && index < filterTags.size() ? index : -1;
    }

    /** The item under the mouse for its tooltip: a grid cell, a basket entry, a filter item or a scanned fluid. */
    private Optional<ItemStack> hoveredItem(double mouseX, double mouseY) {
        Optional<ItemStack> cell = hoveredGridItem(mouseX, mouseY);
        if (cell.isPresent()) {
            return cell;
        }
        int entry = hoveredBasket(mouseX, mouseY);
        if (entry >= 0) {
            return Optional.of(basket().entries().get(entry).key().stack(1));
        }
        int filter = hoveredFilterItem(mouseX, mouseY);
        if (filter >= 0) {
            return Optional.of(filterItems.get(filter));
        }
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        if (view == View.INFO && info != null && info.fluid() && !info.top().isEmpty()
                && inside(mouseX, mouseY, x(LEFT), y(infoGridY(info)), 16, 16)) {
            return Optional.of(info.top().getFirst());
        }
        return Optional.empty();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (amountPopup.isOpen()) {
            return amountPopup.mouseClicked(event, doubleClick);
        }
        if (picker.isOpen()) {
            return picker.mouseClicked(event, doubleClick);
        }
        double mouseX = event.x();
        double mouseY = event.y();
        for (int i = 0; i < SOFTKEYS.length; i++) {
            if (inside(mouseX, mouseY, x(SCREEN_X + i * KEY_STEP), y(KEY_Y), KEY_W, KEY_H)) {
                softkey(SOFTKEYS[i]);
                return true;
            }
        }
        for (CrtButton button : List.copyOf(buttons)) {
            Component label = button.label().get();
            if (button.usable() && inside(mouseX, mouseY, x(button.x()), y(button.y()) - 1, font.width(label) + 6, 11)) {
                button.press().run();
                return true;
            }
        }
        if (clickView(event)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickView(MouseButtonEvent event) {
        double mouseX = event.x();
        double mouseY = event.y();
        switch (view) {
            case ZONES -> {
                return clickZones(mouseX, mouseY);
            }
            case FORM -> {
                return clickForm(mouseX, mouseY);
            }
            case PICKER -> {
                return clickPicker(mouseX, mouseY);
            }
            case INFO, STOCK -> {
                Optional<ItemStack> item = hoveredGridItem(mouseX, mouseY);
                if (item.isPresent()) {
                    clickListItem(item.get(), event.button(), event.hasShiftDown());
                    return true;
                }
                int entry = hoveredBasket(mouseX, mouseY);
                if (entry >= 0) {
                    clickBasket(entry, event.button());
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private boolean clickZones(double mouseX, double mouseY) {
        if (!inside(mouseX, mouseY, x(LEFT), y(LIST_Y), CONTENT_W, ZONE_ROWS * ROW)) {
            return false;
        }
        int index = scroll + (int) ((mouseY - y(LIST_Y)) / ROW);
        List<ZoneList.Zone<ZoneFilter>> rows = zoneRows();
        if (index > rows.size()) {
            return true;
        }
        confirmDelete = ZoneList.NONE;
        ClientPacketDistributor.sendToServer(HandheldZoneActionPayload.pick(index == 0 ? ZoneList.NONE : rows.get(index - 1).id()));
        return true;
    }

    private boolean clickForm(double mouseX, double mouseY) {
        for (int i = 0; i < ZoneList.COLOURS; i++) {
            if (inside(mouseX, mouseY, x(LEFT + i * SWATCH), y(FORM_PALETTE_Y), SWATCH - 2, SWATCH - 2)) {
                colour = i;
                return true;
            }
        }
        if (canAdd() && inside(mouseX, mouseY, x(addX()), y(FORM_ITEMS_LABEL_Y) - 1, font.width(addLabel()) + 6, 11)) {
            picker.open(PickerList.NONE_ID);
            return true;
        }
        int item = hoveredFilterItem(mouseX, mouseY);
        if (item >= 0) {
            filterItems.remove(item);
            return true;
        }
        int tag = hoveredFilterTag(mouseX, mouseY);
        if (tag >= 0) {
            filterTags.remove(tag);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return picker.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return picker.mouseReleased() || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (amountPopup.mouseScrolled(mouseX, mouseY, scrollY) || picker.mouseScrolled(scrollY)) {
            return true;
        }
        int lines = switch (view) {
            case ZONES -> zoneRows().size() + 1 - ZONE_ROWS;
            case INFO, STOCK -> grid().map(Grid::scrollLines).orElse(0);
            case PICKER -> lockerPicker == null ? 0 : lockerPicker.entries().size() - PICKER_ROWS;
            case FORM -> 0;
        };
        if (lines > 0 && inside(mouseX, mouseY, x(SCREEN_X), y(SCREEN_Y), SCREEN_W, SCREEN_H)) {
            scroll = Math.max(0, Math.min(lines, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (amountPopup.keyPressed(event) || picker.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (view == View.PICKER) {
                show(View.STOCK);
                return true;
            }
            return super.keyPressed(event);
        }
        EditBox focused = getFocused() instanceof EditBox box && box.visible ? box : null;
        if (focused != null && (focused.keyPressed(event) || focused.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return amountPopup.charTyped(event) || picker.charTyped(event) || super.charTyped(event);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        amountPopup.tick();
        if (lockerPicker != null && lockerPicker.waiting() && minecraft != null && minecraft.player != null) {
            HandheldLockersPayload lockers = ClientHandheld.takeLockers();
            if (lockers != null) {
                lockerPicker.accept(lockers, minecraft.player.position());
            }
        }
    }
}
