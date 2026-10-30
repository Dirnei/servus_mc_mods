package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.ItemPicker;
import io.github.dirnei.mc.servus.foundation.client.PickerEntries;
import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.foundation.picker.PickerList;
import io.github.dirnei.mc.servus.foundation.picker.TagSearch;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldBinding;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldInfoPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldMode;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldModePayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZoneActionPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZonePayload;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
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
 * picker with {@code #} tag search. FIND lists the bound warehouse's stock;
 * picking an item lights up its shelves. INFO shows the last scanned shelf.
 * Item icons on the screen are tinted green, the hovered one keeps its colours.
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
    private static final int LIST_Y = TOP + 12;
    private static final int CELL = 19;
    private static final float COUNT_SCALE = 0.75F;
    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 6;
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
    private static final HandheldMode[] SOFTKEYS = {HandheldMode.ZONE, HandheldMode.FIND, HandheldMode.INFO};
    private static List<ItemPicker.Entry> itemEntries;

    private enum View {
        ZONES,
        FORM,
        FIND,
        INFO
    }

    /** A text button on the CRT: inverted while hovered, dim while it cannot be used. */
    private record CrtButton(int x, int y, Supplier<Component> label, BooleanSupplier active, Runnable press) {
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

    public HandheldScreen(HandheldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, HEIGHT);
        view = viewOf(menu.handheld().getOrDefault(ServusShuttlesComponents.HANDHELD_MODE.get(), HandheldMode.ZONE));
        picker = new ItemPicker(Component.translatable(KEY + "picker"), this::addToFilter);
        picker.setTagSearch(true);
    }

    private static View viewOf(HandheldMode mode) {
        return switch (mode) {
            case ZONE -> View.ZONES;
            case FIND -> View.FIND;
            case INFO -> View.INFO;
        };
    }

    private HandheldMode modeOf(View view) {
        return switch (view) {
            case ZONES, FORM -> HandheldMode.ZONE;
            case FIND -> HandheldMode.FIND;
            case INFO -> HandheldMode.INFO;
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
        confirmDelete = ZoneList.NONE;
        search.visible = view == View.ZONES || view == View.FIND && findListShown();
        search.setValue("");
        name.visible = view == View.FORM;
        buttons.clear();
        switch (view) {
            case ZONES -> {
                addButton(LEFT, "new", () -> true, () -> openForm(ZoneList.NONE));
                addButton(LEFT + 40, "edit", () -> activeTeamZone() != null, this::editActive);
                buttons.add(new CrtButton(LEFT + 80, BUTTON_Y, this::deleteLabel, () -> activeTeamZone() != null, this::deleteActive));
            }
            case FORM -> {
                addButton(LEFT, "save", () -> problem().isEmpty(), this::saveForm);
                addButton(LEFT + 44, "cancel", () -> true, () -> show(View.ZONES));
            }
            default -> {
            }
        }
        setFocused(view == View.FORM ? name : search.visible ? search : null);
    }

    private void addButton(int x, String key, BooleanSupplier active, Runnable press) {
        Component label = Component.translatable(KEY + key);
        buttons.add(new CrtButton(x, BUTTON_Y, () -> label, active, press));
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
        if (view == View.FIND) {
            search.visible = findListShown();
        }
        int mx = picker.isOpen() ? -1 : mouseX;
        int my = picker.isOpen() ? -1 : mouseY;
        super.extractRenderState(graphics, mx, my, partialTick);
        drawCrtOverlay(graphics);
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
            case FIND -> drawFind(graphics, mouseX, mouseY);
            case INFO -> drawInfo(graphics, mouseX, mouseY);
        }
        for (CrtButton button : buttons) {
            drawButton(graphics, button, mouseX, mouseY);
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

    private void drawFind(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HandheldBinding binding = menu.handheld().get(ServusShuttlesComponents.HANDHELD_BINDING.get());
        String scope = !menu.shelf().isEmpty() ? Component.translatable(KEY + "find_shelf", menu.shelf()).getString()
                : binding == null ? "" : binding.name();
        drawBar(graphics, Component.translatable(KEY + "view.find"), scope);
        if (!findListShown()) {
            graphics.centeredText(font, Component.translatable(KEY + "not_bound"), x(SCREEN_X + SCREEN_W / 2), y(SCREEN_Y + 60), LCD_TEXT);
            graphics.centeredText(font, Component.translatable(KEY + "bind_hint"), x(SCREEN_X + SCREEN_W / 2), y(SCREEN_Y + 74), LCD_DIM);
            return;
        }
        graphics.fill(x(LEFT), y(TOP + 9), x(RIGHT), y(TOP + 10), LCD_LINE);
        List<Integer> shown = stockRows();
        if (shown.isEmpty()) {
            graphics.text(font, Component.translatable(KEY + "find_empty"), x(LEFT), y(LIST_Y + 2), LCD_DIM, false);
        }
        for (int cell = 0; cell < GRID_COLUMNS * GRID_ROWS; cell++) {
            int index = scroll * GRID_COLUMNS + cell;
            if (index >= shown.size()) {
                break;
            }
            int stock = shown.get(index);
            int sx = x(LEFT + (cell % GRID_COLUMNS) * CELL);
            int sy = y(LIST_Y + (cell / GRID_COLUMNS) * CELL);
            boolean hovered = inside(mouseX, mouseY, sx, sy, CELL - 1, CELL - 1);
            if (hovered) {
                graphics.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, LCD_HOVER);
            }
            graphics.outline(sx, sy, CELL - 1, CELL - 1, hovered ? LCD_TEXT : LCD_LINE);
            ItemStack item = menu.stock().get(stock);
            graphics.fakeItem(item, sx + 1, sy + 1);
            drawCount(graphics, CompactCount.slot(menu.stockCounts().get(stock)), sx + CELL - 1, sy + CELL - 1);
        }
    }

    /** Whether FIND shows a list: a scanned shelf's contents, or the bound warehouse's stock. */
    private boolean findListShown() {
        return menu.bound() || !menu.shelf().isEmpty();
    }

    /** A stack count in small CRT text, its bottom right corner at the given point. */
    private void drawCount(GuiGraphicsExtractor graphics, String count, int right, int bottom) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(right - font.width(count) * COUNT_SCALE, bottom - (font.lineHeight - 1) * COUNT_SCALE);
        graphics.pose().scale(COUNT_SCALE, COUNT_SCALE);
        graphics.text(font, count, 0, 0, LCD_TEXT, true);
        graphics.pose().popMatrix();
    }

    private void drawInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        drawBar(graphics, Component.translatable(KEY + "view.info"), info == null || info.code().isEmpty() ? "" : info.code());
        if (info == null) {
            graphics.text(font, Component.translatable(KEY + "info.scan_hint"), x(LEFT), y(TOP + 1), LCD_DIM, false);
            return;
        }
        int line = y(TOP + 1);
        ZoneDefinition zone = ClientZones.definition(info.zoneId());
        if (zone == null) {
            graphics.text(font, Component.translatable(KEY + "no_zone"), x(LEFT), line, LCD_TEXT, false);
        } else {
            graphics.fill(x(LEFT) + 1, line, x(LEFT) + 8, line + 7, ChannelColours.argb(zone.colour()));
            graphics.text(font, zone.name(), x(LEFT) + 11, line, LCD_TEXT, false);
            line += ROW;
            graphics.text(font, font.plainSubstrByWidth(filterSummary(zone.filter()), CONTENT_W), x(LEFT), line, LCD_DIM, false);
        }
        line = y(infoAmountY(info));
        Component amount = info.fluid() ? Component.translatable(KEY + "info.fluid", info.used(), info.total())
                : Component.translatable(KEY + "info.slots", info.used(), info.total());
        graphics.text(font, amount, x(LEFT), line, LCD_TEXT, false);
        line += ROW + 2;
        graphics.text(font, Component.translatable(KEY + "info.top"), x(LEFT), line, LCD_DIM, false);
        graphics.fill(x(LEFT), line + 9, x(RIGHT), line + 10, LCD_LINE);
        line = y(infoRowsY(info));
        if (info.top().isEmpty()) {
            graphics.text(font, Component.translatable(KEY + "info.empty"), x(LEFT), line + 4, LCD_DIM, false);
        }
        if (info.fluid()) {
            for (int i = 0; i < info.top().size(); i++) {
                int rowY = line + i * CELL;
                ItemStack stack = info.top().get(i);
                graphics.fakeItem(stack, x(LEFT), rowY);
                String count = Component.translatable(KEY + "info.mb", info.counts().get(i)).getString();
                String itemName = font.plainSubstrByWidth(stack.getHoverName().getString(), CONTENT_W - 24 - font.width(count));
                graphics.text(font, itemName, x(LEFT) + 20, rowY + 5, LCD_TEXT, false);
                graphics.text(font, count, x(RIGHT) - font.width(count), rowY + 5, LCD_TEXT, false);
            }
            return;
        }
        for (int cell = 0; cell < GRID_COLUMNS * infoGridRows(info); cell++) {
            int index = scroll * GRID_COLUMNS + cell;
            if (index >= info.top().size()) {
                break;
            }
            int sx = x(LEFT + (cell % GRID_COLUMNS) * CELL);
            int sy = line + (cell / GRID_COLUMNS) * CELL;
            boolean hovered = inside(mouseX, mouseY, sx, sy, CELL - 1, CELL - 1);
            if (hovered) {
                graphics.fill(sx, sy, sx + CELL - 1, sy + CELL - 1, LCD_HOVER);
            }
            graphics.outline(sx, sy, CELL - 1, CELL - 1, hovered ? LCD_TEXT : LCD_LINE);
            graphics.fakeItem(info.top().get(index), sx + 1, sy + 1);
            drawCount(graphics, CompactCount.slot(info.counts().get(index)), sx + CELL - 1, sy + CELL - 1);
        }
    }

    /** How many grid rows the INFO grid can scroll down. */
    private static int infoScrollLines() {
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        if (info == null || info.fluid()) {
            return 0;
        }
        return (info.top().size() + GRID_COLUMNS - 1) / GRID_COLUMNS - infoGridRows(info);
    }

    /** How many grid rows of a scanned shelf's items fit on the screen below the readout. */
    private static int infoGridRows(HandheldInfoPayload info) {
        return Math.max(1, (SCREEN_Y + SCREEN_H - 3 - infoRowsY(info)) / CELL);
    }

    /** The index of the scanned shelf's item under the mouse in the INFO grid, or -1. */
    private int hoveredInfoItem(HandheldInfoPayload info, double mouseX, double mouseY) {
        int column = (int) Math.floor((mouseX - x(LEFT)) / CELL);
        int row = (int) Math.floor((mouseY - y(infoRowsY(info))) / CELL);
        if (column < 0 || column >= GRID_COLUMNS || row < 0 || row >= infoGridRows(info)) {
            return -1;
        }
        int index = (scroll + row) * GRID_COLUMNS + column;
        return index < info.top().size() ? index : -1;
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

    private Optional<Integer> hoveredStock(double mouseX, double mouseY) {
        if (view != View.FIND || !menu.bound()) {
            return Optional.empty();
        }
        int column = (int) Math.floor((mouseX - x(LEFT)) / CELL);
        int row = (int) Math.floor((mouseY - y(LIST_Y)) / CELL);
        if (column < 0 || column >= GRID_COLUMNS || row < 0 || row >= GRID_ROWS) {
            return Optional.empty();
        }
        List<Integer> shown = stockRows();
        int index = (scroll + row) * GRID_COLUMNS + column;
        return index < shown.size() ? Optional.of(shown.get(index)) : Optional.empty();
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

    /** The item under the mouse for its tooltip: a stock cell, a filter item or an INFO row. */
    private Optional<ItemStack> hoveredItem(double mouseX, double mouseY) {
        Optional<Integer> stock = hoveredStock(mouseX, mouseY);
        if (stock.isPresent()) {
            return Optional.of(menu.stock().get(stock.get()));
        }
        int filter = hoveredFilterItem(mouseX, mouseY);
        if (filter >= 0) {
            return Optional.of(filterItems.get(filter));
        }
        HandheldInfoPayload info = ClientHandheld.lastInfo();
        if (view == View.INFO && info != null && !info.top().isEmpty()) {
            if (info.fluid()) {
                return inside(mouseX, mouseY, x(LEFT), y(infoRowsY(info)), 16, 16) ? Optional.of(info.top().getFirst()) : Optional.empty();
            }
            int index = hoveredInfoItem(info, mouseX, mouseY);
            if (index >= 0) {
                return Optional.of(info.top().get(index));
            }
        }
        return Optional.empty();
    }

    /** Where the INFO readout's slots line sits: below the zone, and its filter when zoned. */
    private static int infoAmountY(HandheldInfoPayload info) {
        boolean zoned = ClientZones.definition(info.zoneId()) != null;
        return TOP + 1 + (zoned ? ROW : 0) + ROW + 2;
    }

    /** Where the INFO readout's item rows start, below the slots line and the label over them. */
    private static int infoRowsY(HandheldInfoPayload info) {
        return infoAmountY(info) + 2 * ROW + 3;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
            if (button.active().getAsBoolean() && inside(mouseX, mouseY, x(button.x()), y(button.y()) - 1, font.width(label) + 6, 11)) {
                button.press().run();
                return true;
            }
        }
        if (clickView(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickView(double mouseX, double mouseY) {
        switch (view) {
            case ZONES -> {
                return clickZones(mouseX, mouseY);
            }
            case FORM -> {
                return clickForm(mouseX, mouseY);
            }
            case FIND -> {
                Optional<Integer> stock = hoveredStock(mouseX, mouseY);
                if (stock.isPresent()) {
                    ClientPacketDistributor.sendToServer(new HandheldFindPayload(menu.stock().get(stock.get())));
                    onClose();
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
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
        if (picker.mouseScrolled(scrollY)) {
            return true;
        }
        int lines = switch (view) {
            case ZONES -> zoneRows().size() + 1 - ZONE_ROWS;
            case FIND -> (stockRows().size() + GRID_COLUMNS - 1) / GRID_COLUMNS - GRID_ROWS;
            case INFO -> infoScrollLines();
            default -> 0;
        };
        if (lines > 0 && inside(mouseX, mouseY, x(SCREEN_X), y(SCREEN_Y), SCREEN_W, SCREEN_H)) {
            scroll = Math.max(0, Math.min(lines, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (picker.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
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
        return picker.charTyped(event) || super.charTyped(event);
    }
}
