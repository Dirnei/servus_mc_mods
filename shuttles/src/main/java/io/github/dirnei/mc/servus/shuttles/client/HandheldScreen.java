package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.logistics.channel.ChannelColours;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldFindPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZoneActionPayload;
import io.github.dirnei.mc.servus.shuttles.handheld.HandheldZonePayload;
import io.github.dirnei.mc.servus.shuttles.menu.HandheldMenu;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneFilter;
import io.github.dirnei.mc.servus.shuttles.zone.ZoneList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The handheld's GUI. The Zones tab lists the team's zones with a "No zone"
 * entry and a search line; clicking a row makes it the active zone. New and
 * Edit open the zone form (name, colour, nine ghost items, tags), Delete asks
 * once more when loaded bays use the zone. The Find tab lists the bound
 * warehouse's stock; picking an item lights up its bays.
 */
public class HandheldScreen extends AbstractContainerScreen<HandheldMenu> {

    private static final String KEY = "gui.servus_shuttles.handheld.";
    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;
    private static final int MARGIN = 8;
    private static final int TAB_Y = 4;
    private static final int SEARCH_Y = 22;
    private static final int LIST_Y = 38;
    private static final int ROW = 12;
    private static final int ROWS = 9;
    private static final int LIST_WIDTH = WIDTH - 2 * MARGIN;
    private static final int BUTTON_Y = 150;
    private static final int SWATCH_Y = 40;
    private static final int SWATCH = 12;
    private static final int GHOST_Y = 66;
    private static final int TAGS_Y = 88;
    private static final int PROBLEM_Y = 104;
    private static final int CELL = 18;
    private static final int GRID_COLUMNS = 13;
    private static final int GRID_ROWS = 6;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int COLOUR_PROBLEM = 0xFFB02020;
    private static final int COLOUR_ROW_ACTIVE = 0xFF9FD6A6;
    private static final int COLOUR_ROW_HOVER = 0xFFB4B4B4;

    private enum View {
        ZONES,
        FORM,
        FIND
    }

    private View view = View.ZONES;
    private EditBox search;
    private EditBox name;
    private EditBox tags;
    private Button zonesTab;
    private Button findTab;
    private Button create;
    private Button edit;
    private Button delete;
    private Button save;
    private Button cancel;
    private int scroll;
    /** The zone the form edits, 0 for a new one. */
    private int editing;
    private int colour;
    private final ItemStack[] ghosts = new ItemStack[ZoneFilter.SLOTS];
    /** The zone whose delete waits for a second click, 0 for none. */
    private int confirmDelete;

    public HandheldScreen(HandheldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WIDTH, HEIGHT);
        Arrays.fill(ghosts, ItemStack.EMPTY);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelY = -1000;
        inventoryLabelX = HandheldMenu.INVENTORY_X;
        inventoryLabelY = HandheldMenu.INVENTORY_Y - 11;
        zonesTab = addRenderableWidget(Button.builder(Component.translatable(KEY + "tab.zones"), button -> show(View.ZONES))
                .bounds(leftPos + MARGIN, topPos + TAB_Y, 60, 14).build());
        findTab = addRenderableWidget(Button.builder(Component.translatable(KEY + "tab.find"), button -> show(View.FIND))
                .bounds(leftPos + MARGIN + 64, topPos + TAB_Y, 60, 14).build());
        search = addRenderableWidget(box(SEARCH_Y, LIST_WIDTH, 50, "search"));
        search.setResponder(text -> scroll = 0);
        name = addRenderableWidget(box(SEARCH_Y, 160, ZoneList.MAX_NAME, "name"));
        tags = addRenderableWidget(box(TAGS_Y, LIST_WIDTH, 200, "tags"));
        create = addRenderableWidget(button(0, "new", button -> openForm(ZoneList.NONE)));
        edit = addRenderableWidget(button(1, "edit", button -> editActive()));
        delete = addRenderableWidget(button(2, "delete", button -> deleteActive()));
        save = addRenderableWidget(button(0, "save", button -> saveForm()));
        cancel = addRenderableWidget(button(1, "cancel", button -> show(View.ZONES)));
        show(view);
    }

    private EditBox box(int y, int width, int maxLength, String key) {
        EditBox box = new EditBox(font, leftPos + MARGIN, topPos + y, width, 12, Component.translatable(KEY + key));
        box.setMaxLength(maxLength);
        box.setHint(Component.translatable(KEY + key + "_hint"));
        return box;
    }

    private Button button(int index, String key, Button.OnPress press) {
        return Button.builder(Component.translatable(KEY + key), press).bounds(leftPos + MARGIN + index * 60, topPos + BUTTON_Y, 56, 14).build();
    }

    private void show(View next) {
        view = next;
        scroll = 0;
        confirmDelete = ZoneList.NONE;
        boolean list = view == View.ZONES;
        boolean form = view == View.FORM;
        search.visible = list || view == View.FIND && menu.bound();
        search.setValue("");
        name.visible = form;
        tags.visible = form;
        create.visible = list;
        edit.visible = list;
        delete.visible = list;
        save.visible = form;
        cancel.visible = form;
        zonesTab.active = view != View.ZONES;
        findTab.active = view != View.FIND;
        setFocused(form ? name : list ? search : null);
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
        tags.setValue(zone == null ? "" : zone.filter().tagLine());
        colour = zone == null ? 0 : zone.colour();
        Arrays.fill(ghosts, ItemStack.EMPTY);
        if (zone != null) {
            for (int i = 0; i < Math.min(ghosts.length, zone.filter().items().size()); i++) {
                ghosts[i] = zone.filter().items().get(i).copyWithCount(1);
            }
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
        if (menu.bayCount(zone.id()) > 0 && confirmDelete != zone.id()) {
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
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack ghost : ghosts) {
            if (!ghost.isEmpty()) {
                items.add(ghost);
            }
        }
        ZoneFilter filter = new ZoneFilter(items, ZoneFilter.parseTags(tags.getValue()));
        ClientPacketDistributor.sendToServer(new HandheldZonePayload(editing, name.getValue(), colour, filter));
        show(View.ZONES);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ZoneList.Zone<ZoneFilter> active = activeTeamZone();
        edit.active = active != null;
        delete.active = active != null;
        delete.setMessage(Component.translatable(KEY + (active != null && confirmDelete == active.id() ? "delete_confirm" : "delete")));
        save.active = problem().isEmpty();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (view == View.FIND) {
            hoveredStock(mouseX, mouseY).ifPresent(index -> graphics.setTooltipForNextFrame(font, menu.stock().get(index), mouseX, mouseY));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        PanelPainter.playerInventory(graphics, leftPos, topPos, HandheldMenu.INVENTORY_X, HandheldMenu.INVENTORY_Y, HandheldMenu.INVENTORY_Y + 58);
        switch (view) {
            case ZONES -> drawZones(graphics, mouseX, mouseY);
            case FORM -> drawForm(graphics);
            case FIND -> drawFind(graphics);
        }
    }

    private void drawZones(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = leftPos + MARGIN;
        int y = topPos + LIST_Y;
        PanelPainter.sunken(graphics, x - 1, y - 1, LIST_WIDTH + 2, ROWS * ROW + 2);
        List<ZoneList.Zone<ZoneFilter>> rows = zoneRows();
        Integer active = menu.activeZone();
        for (int row = 0; row < ROWS; row++) {
            int index = scroll + row;
            if (index > rows.size()) {
                break;
            }
            int top = y + row * ROW;
            ZoneList.Zone<ZoneFilter> zone = index == 0 ? null : rows.get(index - 1);
            int id = zone == null ? ZoneList.NONE : zone.id();
            boolean isActive = active != null && active == id;
            if (isActive || inside(mouseX, mouseY, x, top, LIST_WIDTH, ROW)) {
                graphics.fill(x, top, x + LIST_WIDTH, top + ROW, isActive ? COLOUR_ROW_ACTIVE : COLOUR_ROW_HOVER);
            }
            if (zone == null) {
                graphics.text(font, Component.translatable(KEY + "no_zone"), x + 14, top + 2, COLOUR_TEXT, false);
                continue;
            }
            graphics.fill(x + 2, top + 2, x + 10, top + 10, ChannelColours.argb(zone.colour()));
            graphics.text(font, zone.name(), x + 14, top + 2, COLOUR_TEXT, false);
            String summary = font.plainSubstrByWidth(filterSummary(zone.filter()), 80);
            graphics.text(font, summary, x + 130, top + 2, COLOUR_DIM, false);
            String count = Integer.toString(menu.bayCount(zone.id()));
            graphics.text(font, count, x + LIST_WIDTH - 2 - font.width(count), top + 2, COLOUR_DIM, false);
        }
    }

    /** The filter in a few words: its tags and item names, or "anything". */
    static String filterSummary(ZoneFilter filter) {
        if (filter.isEmpty()) {
            return Component.translatable(KEY + "takes_anything").getString();
        }
        List<String> parts = new ArrayList<>(filter.tags().stream().map(tag -> "#" + tag).toList());
        parts.addAll(filter.itemNames());
        return String.join(", ", parts);
    }

    private void drawForm(GuiGraphicsExtractor graphics) {
        int x = leftPos + MARGIN;
        for (int i = 0; i < ZoneList.COLOURS; i++) {
            int sx = x + i * SWATCH;
            int sy = topPos + SWATCH_Y;
            graphics.fill(sx, sy, sx + SWATCH - 2, sy + SWATCH - 2, ChannelColours.argb(i));
            if (i == colour) {
                graphics.outline(sx - 1, sy - 1, SWATCH, SWATCH, 0xFFFFFFFF);
            }
        }
        for (int i = 0; i < ghosts.length; i++) {
            int sx = x + i * CELL;
            int sy = topPos + GHOST_Y;
            PanelPainter.sunken(graphics, sx - 1, sy - 1, CELL, CELL);
            graphics.fakeItem(ghosts[i], sx, sy);
        }
        problem().ifPresent(problem -> graphics.text(font, Component.translatable(KEY + "problem." + problem.name().toLowerCase(Locale.ROOT)),
                x, topPos + PROBLEM_Y, COLOUR_PROBLEM, false));
    }

    private void drawFind(GuiGraphicsExtractor graphics) {
        int x = leftPos + MARGIN;
        int y = topPos + LIST_Y;
        if (!menu.bound()) {
            graphics.centeredText(font, Component.translatable(KEY + "not_bound"), leftPos + imageWidth / 2, topPos + 70, COLOUR_TEXT);
            return;
        }
        PanelPainter.sunken(graphics, x - 1, y - 1, GRID_COLUMNS * CELL + 2, GRID_ROWS * CELL + 2);
        List<Integer> shown = stockRows();
        for (int cell = 0; cell < GRID_COLUMNS * GRID_ROWS; cell++) {
            int index = scroll * GRID_COLUMNS + cell;
            if (index >= shown.size()) {
                break;
            }
            int stock = shown.get(index);
            int sx = x + (cell % GRID_COLUMNS) * CELL + 1;
            int sy = y + (cell / GRID_COLUMNS) * CELL + 1;
            ItemStack item = menu.stock().get(stock);
            graphics.fakeItem(item, sx, sy);
            graphics.itemDecorations(font, item, sx, sy, CompactCount.of(menu.stockCounts().get(stock)));
        }
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
        if (!menu.bound()) {
            return Optional.empty();
        }
        int column = (int) Math.floor((mouseX - leftPos - MARGIN) / CELL);
        int row = (int) Math.floor((mouseY - topPos - LIST_Y) / CELL);
        if (column < 0 || column >= GRID_COLUMNS || row < 0 || row >= GRID_ROWS) {
            return Optional.empty();
        }
        List<Integer> shown = stockRows();
        int index = (scroll + row) * GRID_COLUMNS + column;
        return index < shown.size() ? Optional.of(shown.get(index)) : Optional.empty();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (view == View.ZONES && clickZones(event.x(), event.y())) {
            return true;
        }
        if (view == View.FORM && clickForm(event.x(), event.y())) {
            return true;
        }
        if (view == View.FIND) {
            Optional<Integer> stock = hoveredStock(event.x(), event.y());
            if (stock.isPresent()) {
                ClientPacketDistributor.sendToServer(new HandheldFindPayload(menu.stock().get(stock.get())));
                onClose();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean clickZones(double mouseX, double mouseY) {
        int x = leftPos + MARGIN;
        int y = topPos + LIST_Y;
        if (!inside(mouseX, mouseY, x, y, LIST_WIDTH, ROWS * ROW)) {
            return false;
        }
        int index = scroll + (int) ((mouseY - y) / ROW);
        List<ZoneList.Zone<ZoneFilter>> rows = zoneRows();
        if (index > rows.size()) {
            return true;
        }
        confirmDelete = ZoneList.NONE;
        ClientPacketDistributor.sendToServer(HandheldZoneActionPayload.pick(index == 0 ? ZoneList.NONE : rows.get(index - 1).id()));
        return true;
    }

    private boolean clickForm(double mouseX, double mouseY) {
        int x = leftPos + MARGIN;
        for (int i = 0; i < ZoneList.COLOURS; i++) {
            if (inside(mouseX, mouseY, x + i * SWATCH, topPos + SWATCH_Y, SWATCH - 2, SWATCH - 2)) {
                colour = i;
                return true;
            }
        }
        for (int i = 0; i < ghosts.length; i++) {
            if (inside(mouseX, mouseY, x + i * CELL - 1, topPos + GHOST_Y - 1, CELL, CELL)) {
                ItemStack carried = menu.getCarried();
                ghosts[i] = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
                return true;
            }
        }
        return false;
    }

    /** In the form, shift clicking an inventory item puts it into the first free ghost slot instead of moving it. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int buttonNum, ContainerInput containerInput) {
        if (view == View.FORM && containerInput == ContainerInput.QUICK_MOVE && slot != null && slot.hasItem()) {
            for (int i = 0; i < ghosts.length; i++) {
                if (ghosts[i].isEmpty()) {
                    ghosts[i] = slot.getItem().copyWithCount(1);
                    break;
                }
            }
            return;
        }
        super.slotClicked(slot, slotId, buttonNum, containerInput);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int lines = view == View.ZONES ? zoneRows().size() + 1 - ROWS
                : view == View.FIND ? (stockRows().size() + GRID_COLUMNS - 1) / GRID_COLUMNS - GRID_ROWS : 0;
        if (lines > 0 && mouseY < topPos + BUTTON_Y) {
            scroll = Math.max(0, Math.min(lines, scroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
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
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        if (view == View.FORM) {
            graphics.text(font, Component.translatable(KEY + "items"), MARGIN, GHOST_Y - 10, COLOUR_TEXT, false);
        }
    }
}
