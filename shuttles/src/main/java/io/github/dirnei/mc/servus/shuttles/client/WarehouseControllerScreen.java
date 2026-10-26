package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.Scrollbar;
import io.github.dirnei.mc.servus.foundation.client.TitleNameEditor;
import io.github.dirnei.mc.servus.foundation.picker.PickerStock;
import io.github.dirnei.mc.servus.shuttles.controller.ControllerEnergy;
import io.github.dirnei.mc.servus.shuttles.controller.RenameSitePayload;
import io.github.dirnei.mc.servus.shuttles.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.StorageRow;
import io.github.dirnei.mc.servus.shuttles.site.ChunkMapLayout;
import io.github.dirnei.mc.servus.shuttles.site.SiteCapacity;
import io.github.dirnei.mc.servus.shuttles.site.SiteName;
import io.github.dirnei.mc.servus.shuttles.terminal.SiteRow;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** The site GUI of a Warehouse Controller: name, status, FE, chunk map, storage fill, Anchor Upgrades, Wireless Upgrade and other sites. */
public class WarehouseControllerScreen extends AbstractContainerScreen<WarehouseControllerMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_NO_TRADE = 0xFFD07020;
    private static final int COLOUR_GOOD = 0xFF2E7D32;
    private static final int COLOUR_BAD = 0xFFB71C1C;
    private static final int COLOUR_DIM = 0xFF707070;
    private static final int CELL = 0xFF4E8A5A;
    private static final int CELL_OVER = 0xFFC0392B;
    private static final int CELL_HOME = 0xFFE0B040;
    private static final int MAP_BACKGROUND = 0xFF2A2D33;
    private static final int BAR_ITEMS = 0xFF2E8B7A;
    private static final int BAR_FLUIDS = 0xFF3A6EC0;
    private static final int STORAGE_WIDTH = WarehouseControllerMenu.WIDTH - 16;
    private static final int STATUS_WIDTH = WarehouseControllerMenu.WIDTH - WarehouseControllerMenu.STATUS_X - 6;
    private static final int UPGRADES_LABEL_Y = WarehouseControllerMenu.UPGRADES_Y - 12;
    /** The side panel with the team's other sites, right of the GUI. */
    private static final int SITES_GAP = 2;
    private static final int SITES_WIDTH = 120;
    private static final int SITE_ROW = 11;
    private static final int SITES_MAX = 16;
    /** Right edge of a site row's name, left of the scrollbar. */
    private static final int SITE_NAME_RIGHT = SITES_WIDTH - 13;

    private TitleNameEditor nameEditor;
    /** Where the online line was drawn, for its summary tooltip; -1 when not drawn. */
    private int onlineLineY = -1;
    /** Where the FE line was drawn, for its tooltip; -1 when not drawn. */
    private int energyLineY = -1;
    private final Scrollbar sitesBar = new Scrollbar();

    public WarehouseControllerScreen(WarehouseControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WarehouseControllerMenu.WIDTH, WarehouseControllerMenu.IMAGE_HEIGHT);
        ClientSiteStatus.clear();
    }

    /** The name is the title, renamed with the pencil after it; the default name shows until one is typed. */
    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = WarehouseControllerMenu.INVENTORY_Y - 11;
        if (nameEditor == null) {
            nameEditor = new TitleNameEditor(SiteName.clean(menu.name()),
                    Component.literal(SiteName.display("", menu.pos().getX(), menu.pos().getZ())),
                    Component.translatable("gui.servus_shuttles.controller.name"), SiteName.MAX_LENGTH, SiteName::clean,
                    typed -> ClientPacketDistributor.sendToServer(new RenameSitePayload(typed)), this::setFocused);
        }
        addRenderableWidget(nameEditor.createBox(font));
        addRenderableWidget(nameEditor.createButton());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        nameEditor.tick();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return nameEditor.keyPressed(event) || super.keyPressed(event);
    }

    @Override
    public void removed() {
        nameEditor.removed();
        super.removed();
    }

    private Rect upgradesRow() {
        return new Rect(menu.upgradeX(0), WarehouseControllerMenu.UPGRADES_Y, menu.upgradeSlots() * 18, 18);
    }

    private static Rect wirelessSlot() {
        return new Rect(WarehouseControllerMenu.WIRELESS_X, WarehouseControllerMenu.UPGRADES_Y, 18, 18);
    }

    private static Rect map() {
        return new Rect(WarehouseControllerMenu.MAP_X, WarehouseControllerMenu.MAP_Y, WarehouseControllerMenu.MAP_SIZE, WarehouseControllerMenu.MAP_SIZE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        nameEditor.layout(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        SiteStatusPayload status = ClientSiteStatus.status();
        Rect wireless = wirelessSlot();
        boolean overWireless = x >= wireless.x() - 1 && x < wireless.right() && y >= UPGRADES_LABEL_Y && y < wireless.bottom();
        boolean emptyHand = menu.getCarried().isEmpty() && (hoveredSlot == null || !hoveredSlot.hasItem());
        SiteRow site = siteUnder(mouseX, mouseY, status);
        if (emptyHand && upgradesRow().coversRow(x, y, UPGRADES_LABEL_Y)) {
            wrappedTooltip(graphics, "gui.servus_shuttles.controller.upgrades.tooltip", mouseX, mouseY);
        } else if (emptyHand && overWireless) {
            wrappedTooltip(graphics, "gui.servus_shuttles.controller.wireless.tooltip", mouseX, mouseY);
        } else if (overStatusLine(energyLineY, x, y)) {
            wrappedTooltip(graphics, "gui.servus_shuttles.controller.energy.tooltip", mouseX, mouseY);
        } else if (map().contains(x, y)) {
            wrappedTooltip(graphics, "gui.servus_shuttles.controller.map.tooltip", mouseX, mouseY);
        } else if (site != null) {
            List<Component> lines = List.of(Component.literal(site.name()),
                    Component.translatable("gui.servus_shuttles.site.state." + site.remoteState().name().toLowerCase(Locale.ROOT)));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else if (!status.storage().equals(StorageRow.EMPTY) && overStorageLine(0, x, y)) {
            graphics.setComponentTooltipForNextFrame(font, itemsTooltip(status.storage()), mouseX, mouseY);
        } else if (!status.storage().equals(StorageRow.EMPTY) && overStorageLine(1, x, y)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.servus_shuttles.storage.gantries",
                    status.storage().gantries())), mouseX, mouseY);
        } else if (overStatusLine(onlineLineY, x, y) && !status.summary().getString().isEmpty()) {
            graphics.setTooltipForNextFrame(font, font.split(status.summary(), 200), mouseX, mouseY);
        }
    }

    private void wrappedTooltip(GuiGraphicsExtractor graphics, String key, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, font.split(Component.translatable(key), 180), mouseX, mouseY);
    }

    /** Whether a point is on storage line {@code index} (0 Items, 1 Fluids) with its bar. */
    private static boolean overStorageLine(int index, int x, int y) {
        int top = WarehouseControllerMenu.STORAGE_Y + index * WarehouseControllerMenu.STORAGE_LINE;
        return x >= 8 && x < 8 + STORAGE_WIDTH && y >= top && y < top + WarehouseControllerMenu.STORAGE_LINE;
    }

    /** The rack bays that count and the full zones. */
    private static List<Component> itemsTooltip(StorageRow storage) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.servus_shuttles.storage.bays", storage.bays()));
        List<String> full = storage.fullZones();
        if (!full.isEmpty()) {
            lines.add(Component.translatable("gui.servus_shuttles.storage.full_zones", String.join(", ", full)).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    /** Whether a point is on a status line drawn at {@code lineY}; -1 means the line was not drawn. */
    private static boolean overStatusLine(int lineY, int x, int y) {
        return lineY >= 0 && x >= WarehouseControllerMenu.STATUS_X && y >= lineY && y < lineY + 9;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.slots(graphics, leftPos, topPos, wirelessSlot(), 1, 1);
        PanelPainter.playerInventory(graphics, leftPos, topPos, WarehouseControllerMenu.INVENTORY_Y, menu.hotbarY());
        renderMap(graphics, ClientSiteStatus.status());
        renderSites(graphics, ClientSiteStatus.status(), mouseX, mouseY);
    }

    /** The site panel right of the GUI, so JEI keeps its item list clear of it. */
    public List<Rect2i> extraAreas() {
        SiteStatusPayload status = ClientSiteStatus.status();
        if (status == SiteStatusPayload.EMPTY) {
            return List.of();
        }
        return List.of(new Rect2i(sitesLeft(), topPos, SITES_WIDTH, sitesHeight(status)));
    }

    /** A click on the site panel is not a click outside the GUI, so a carried stack is not dropped. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean onPanel = extraAreas().stream().anyMatch(area -> mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight());
        return !onPanel && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    /** The hint the panel shows instead of sites while the controller is not wireless capable, empty otherwise. */
    private static Component hint(SiteStatusPayload status) {
        return switch (status.hint()) {
            case SiteStatusPayload.HINT_NO_UPGRADE -> Component.translatable("gui.servus_shuttles.controller.sites.no_upgrade");
            case SiteStatusPayload.HINT_NO_POWER -> Component.translatable("gui.servus_shuttles.controller.sites.no_power", status.needs());
            default -> Component.empty();
        };
    }

    /** Rows of the side panel: one per site, or the lines of the hint. */
    private int panelRows(SiteStatusPayload status) {
        if (!status.capable()) {
            return Math.max(1, font.split(hint(status), SITES_WIDTH - 12).size());
        }
        return Math.max(1, Math.min(SITES_MAX, status.sites().size()));
    }

    private int sitesHeight(SiteStatusPayload status) {
        return 18 + panelRows(status) * SITE_ROW + 4;
    }

    private int sitesLeft() {
        return leftPos + imageWidth + SITES_GAP;
    }

    /** The side panel: a title, then one row per site with a state mark and its name, orange when it cannot trade. */
    private void renderSites(GuiGraphicsExtractor graphics, SiteStatusPayload status, int mouseX, int mouseY) {
        if (status == SiteStatusPayload.EMPTY) {
            return;
        }
        int rows = panelRows(status);
        int left = sitesLeft();
        PanelPainter.panel(graphics, left, topPos, SITES_WIDTH, sitesHeight(status));
        graphics.text(font, Component.translatable("gui.servus_shuttles.controller.sites"), left + 6, topPos + 6, COLOUR_TEXT, false);
        if (!status.capable()) {
            int y = topPos + 19;
            for (FormattedCharSequence line : font.split(hint(status), SITES_WIDTH - 12)) {
                graphics.text(font, line, left + 6, y, COLOUR_NO_TRADE, false);
                y += SITE_ROW;
            }
            return;
        }
        if (status.sites().isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.controller.sites.none"), left + 6, topPos + 19, COLOUR_DIM, false);
            return;
        }
        Scrollbar bar = syncSitesBar(status);
        bar.render(graphics, mouseX, mouseY);
        for (int i = 0; i < rows; i++) {
            SiteRow site = status.sites().get(bar.offset() + i);
            int y = topPos + 18 + i * SITE_ROW;
            int mark = switch (site.remoteState()) {
                case ONLINE -> 0xFF40B040;
                case SLEEPING -> 0xFFE0B040;
                case UNREACHABLE -> 0xFFC03030;
            };
            graphics.fill(left + 6, y + 1, left + 12, y + 7, mark);
            String siteName = font.plainSubstrByWidth(site.name(), SITE_NAME_RIGHT - 15);
            graphics.text(font, siteName, left + 15, y, site.trade() ? COLOUR_TEXT : COLOUR_NO_TRADE, false);
        }
    }

    private SiteRow siteUnder(int mouseX, int mouseY, SiteStatusPayload status) {
        int x = mouseX - sitesLeft();
        int y = mouseY - topPos - 18;
        if (!status.capable() || x < 0 || x >= SITE_NAME_RIGHT || y < 0) {
            return null;
        }
        int row = y / SITE_ROW;
        return row < Math.min(SITES_MAX, status.sites().size()) ? status.sites().get(syncSitesBar(status).offset() + row) : null;
    }

    /** The site list's scrollbar beside the rows, set to the listed sites. */
    private Scrollbar syncSitesBar(SiteStatusPayload status) {
        sitesBar.setArea(sitesLeft() + SITES_WIDTH - 10, topPos + 19, Scrollbar.NARROW, panelRows(status) * SITE_ROW - 2);
        sitesBar.setContent(status.sites().size(), SITES_MAX);
        return sitesBar;
    }

    /** Whether the site list with its scrollbar shows. */
    private static boolean listsSites(SiteStatusPayload status) {
        return status != SiteStatusPayload.EMPTY && status.capable() && !status.sites().isEmpty();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (nameEditor.mouseClicked(event)) {
            return true;
        }
        SiteStatusPayload status = ClientSiteStatus.status();
        if (listsSites(status) && syncSitesBar(status).mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return sitesBar.mouseDragged(event.y()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return sitesBar.mouseReleased() || super.mouseReleased(event);
    }

    /** The wheel over the site panel scrolls the site list one row. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        SiteStatusPayload status = ClientSiteStatus.status();
        boolean overPanel = mouseX >= sitesLeft() && mouseX < sitesLeft() + SITES_WIDTH && mouseY >= topPos
                && mouseY < topPos + sitesHeight(status);
        if (listsSites(status) && overPanel && scrollY != 0) {
            syncSitesBar(status).scrollBy(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderMap(GuiGraphicsExtractor graphics, SiteStatusPayload status) {
        Rect map = map();
        PanelPainter.inset(graphics, leftPos, topPos, map);
        int left = leftPos + map.x();
        int top = topPos + map.y();
        graphics.fill(left, top, left + map.w(), top + map.h(), MAP_BACKGROUND);
        if (status == SiteStatusPayload.EMPTY) {
            return;
        }
        boolean over = status.capacityState() == SiteCapacity.State.OVER;
        ChunkMapLayout.Layout layout = ChunkMapLayout.layout(status.chunks(), status.home(), map.w());
        int cell = layout.cell();
        int gap = cell >= 4 ? 1 : 0;
        for (ChunkMapLayout.Cell c : layout.cells()) {
            int colour = c.home() ? CELL_HOME : over ? CELL_OVER : CELL;
            graphics.fill(left + c.x(), top + c.y(), left + c.x() + cell - gap, top + c.y() + cell - gap, colour);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        SiteStatusPayload status = ClientSiteStatus.status();
        nameEditor.drawTitle(graphics, font, leftPos, topPos);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, COLOUR_TEXT, false);
        Component upgrades = Component.translatable("gui.servus_shuttles.controller.upgrades");
        graphics.text(font, upgrades, menu.upgradeX(0) - 1, WarehouseControllerMenu.UPGRADES_Y - 11, COLOUR_TEXT, false);
        Component wireless = Component.translatable("gui.servus_shuttles.controller.wireless");
        graphics.text(font, wireless, WarehouseControllerMenu.WIRELESS_X + 17 - font.width(wireless), WarehouseControllerMenu.UPGRADES_Y - 11,
                COLOUR_TEXT, false);
        onlineLineY = -1;
        energyLineY = -1;
        if (status != SiteStatusPayload.EMPTY) {
            renderStatus(graphics, status);
        }
        if (!status.storage().equals(StorageRow.EMPTY)) {
            renderStorage(graphics, status.storage());
        }
    }

    /** The Items and Fluids lines below the map, each with a bar under its text. */
    private void renderStorage(GuiGraphicsExtractor graphics, StorageRow storage) {
        int y = WarehouseControllerMenu.STORAGE_Y;
        if (storage.total() > 0) {
            storageLine(graphics, Component.translatable("gui.servus_shuttles.storage.items", PickerStock.shortCount(storage.used()),
                    PickerStock.shortCount(storage.total())), y, storage.used(), storage.total(), storage.full() ? COLOUR_BAD : BAR_ITEMS);
        } else {
            graphics.text(font, Component.translatable("gui.servus_shuttles.storage.no_racks"), 8, y, COLOUR_DIM, false);
        }
        y += WarehouseControllerMenu.STORAGE_LINE;
        if (storage.fluidCapacity() > 0) {
            storageLine(graphics, Component.translatable("gui.servus_shuttles.storage.fluids", PickerStock.shortCount(storage.fluid()),
                    PickerStock.shortCount(storage.fluidCapacity())), y, storage.fluid(), storage.fluidCapacity(),
                    storage.fluidFull() ? COLOUR_BAD : BAR_FLUIDS);
        } else {
            graphics.text(font, Component.translatable("gui.servus_shuttles.storage.no_gantry"), 8, y, COLOUR_DIM, false);
        }
    }

    private void storageLine(GuiGraphicsExtractor graphics, Component text, int y, long value, long max, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text.getString(), STORAGE_WIDTH), 8, y, COLOUR_TEXT, false);
        PanelPainter.bar(graphics, 8, y + 10, STORAGE_WIDTH, 4, value, max, colour, PanelPainter.DARK);
    }

    /** The status column right of the map, each line wrapped; the empty upgrade slots speak for a missing upgrade. */
    private void renderStatus(GuiGraphicsExtractor graphics, SiteStatusPayload status) {
        int y = WarehouseControllerMenu.MAP_Y;
        Component online = switch (status.online()) {
            case 0 -> Component.translatable("gui.servus_shuttles.controller.online");
            case 2 -> Component.translatable("gui.servus_shuttles.controller.offline_multiple");
            default -> Component.translatable("gui.servus_shuttles.controller.offline_none");
        };
        onlineLineY = y;
        y = lines(graphics, online, y, status.online() == 0 ? COLOUR_GOOD : COLOUR_BAD);
        y = lines(graphics, Component.translatable("gui.servus_shuttles.controller.chunks", status.chunks().size()), y, COLOUR_TEXT);
        switch (status.capacityState()) {
            case COVERED -> y = lines(graphics, Component.translatable("gui.servus_shuttles.controller.covered",
                    status.chunks().size(), status.capacity()), y, COLOUR_GOOD);
            case OVER -> y = lines(graphics, Component.translatable("gui.servus_shuttles.controller.over",
                    status.chunks().size(), status.capacity()), y, COLOUR_BAD);
            case NO_UPGRADE -> {
            }
        }
        energyLineY = y;
        boolean energyOk = !status.noPower() && (status.capable() || status.hint() != SiteStatusPayload.HINT_NO_POWER);
        y = lines(graphics, Component.translatable("gui.servus_shuttles.controller.energy_use", ControllerEnergy.format(status.energy()),
                ControllerEnergy.format(status.energyCapacity()), ControllerEnergy.format(status.used())), y, energyOk ? COLOUR_TEXT : COLOUR_BAD);
        if (status.noPower()) {
            y = lines(graphics, Component.translatable("gui.servus_shuttles.controller.no_power"), y, COLOUR_BAD);
        }
        if (!status.wake().getString().isEmpty()) {
            lines(graphics, status.wake(), y, status.wakeAlert() ? COLOUR_BAD : COLOUR_TEXT);
        }
    }

    /** Draws a wrapped text at the status column above the upgrade label; returns the y below it. */
    private int lines(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        for (FormattedCharSequence line : font.split(text, STATUS_WIDTH)) {
            if (y > WarehouseControllerMenu.STORAGE_Y - 10) {
                break;
            }
            graphics.text(font, line, WarehouseControllerMenu.STATUS_X, y, colour, false);
            y += 9;
        }
        return y + 1;
    }
}
