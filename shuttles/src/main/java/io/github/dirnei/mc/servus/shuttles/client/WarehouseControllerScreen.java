package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.controller.ControllerEnergy;
import io.github.dirnei.mc.servus.shuttles.controller.RenameSitePayload;
import io.github.dirnei.mc.servus.shuttles.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.shuttles.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.shuttles.site.ChunkMapLayout;
import io.github.dirnei.mc.servus.shuttles.site.SiteCapacity;
import io.github.dirnei.mc.servus.shuttles.site.SiteName;
import io.github.dirnei.mc.servus.shuttles.terminal.SiteRow;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/** The site GUI of a Warehouse Controller: name, status, FE, chunk map, Anchor Upgrades, Wireless Upgrade and other sites. */
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
    private static final int STATUS_WIDTH = WarehouseControllerMenu.WIDTH - WarehouseControllerMenu.STATUS_X - 6;
    private static final int UPGRADES_LABEL_Y = WarehouseControllerMenu.UPGRADES_Y - 12;
    /** The side panel with the team's other sites, right of the GUI. */
    private static final int SITES_GAP = 2;
    private static final int SITES_WIDTH = 120;
    private static final int SITE_ROW = 11;
    private static final int SITES_MAX = 16;

    private EditBox name;
    private boolean nameWasFocused;
    private String sentName;
    /** Where the online line was drawn, for its summary tooltip; -1 when not drawn. */
    private int onlineLineY = -1;
    /** Where the FE line was drawn, for its tooltip; -1 when not drawn. */
    private int energyLineY = -1;

    public WarehouseControllerScreen(WarehouseControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WarehouseControllerMenu.WIDTH, WarehouseControllerMenu.IMAGE_HEIGHT);
        ClientSiteStatus.clear();
    }

    /** The name is the title: a borderless field in the title row, showing the default name until one is typed. */
    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = WarehouseControllerMenu.INVENTORY_Y - 11;
        name = new EditBox(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX, 10,
                Component.translatable("gui.servus_shuttles.controller.name"));
        name.setMaxLength(SiteName.MAX_LENGTH);
        name.setHint(Component.literal(SiteName.display("", menu.pos().getX(), menu.pos().getZ())));
        name.setBordered(false);
        name.setTextColor(COLOUR_TEXT);
        name.setTextShadow(false);
        name.setTooltip(Tooltip.create(Component.translatable("gui.servus_shuttles.controller.name_hint")));
        name.setValue(menu.name());
        sentName = menu.name();
        addRenderableWidget(name);
    }

    private void sendName() {
        String typed = name.getValue();
        if (!SiteName.clean(typed).equals(SiteName.clean(sentName))) {
            sentName = typed;
            ClientPacketDistributor.sendToServer(new RenameSitePayload(typed));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean focused = name.isFocused();
        if (nameWasFocused && !focused) {
            sendName();
        }
        nameWasFocused = focused;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            sendName();
            return super.keyPressed(event);
        }
        if (name.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendName();
                name.setFocused(false);
                return true;
            }
            if (name.keyPressed(event) || name.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        sendName();
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
        } else if (overStatusLine(onlineLineY, x, y) && !status.summary().getString().isEmpty()) {
            graphics.setTooltipForNextFrame(font, font.split(status.summary(), 200), mouseX, mouseY);
        }
    }

    private void wrappedTooltip(GuiGraphicsExtractor graphics, String key, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(font, font.split(Component.translatable(key), 180), mouseX, mouseY);
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
        renderSites(graphics, ClientSiteStatus.status());
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
    private void renderSites(GuiGraphicsExtractor graphics, SiteStatusPayload status) {
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
        for (int i = 0; i < rows; i++) {
            SiteRow site = status.sites().get(i);
            int y = topPos + 18 + i * SITE_ROW;
            int mark = switch (site.remoteState()) {
                case ONLINE -> 0xFF40B040;
                case SLEEPING -> 0xFFE0B040;
                case UNREACHABLE -> 0xFFC03030;
            };
            graphics.fill(left + 6, y + 1, left + 12, y + 7, mark);
            String siteName = font.plainSubstrByWidth(site.name(), SITES_WIDTH - 21);
            graphics.text(font, siteName, left + 15, y, site.trade() ? COLOUR_TEXT : COLOUR_NO_TRADE, false);
        }
    }

    private SiteRow siteUnder(int mouseX, int mouseY, SiteStatusPayload status) {
        int x = mouseX - sitesLeft();
        int y = mouseY - topPos - 18;
        if (x < 0 || x >= SITES_WIDTH || y < 0) {
            return null;
        }
        int row = y / SITE_ROW;
        return row < Math.min(SITES_MAX, status.sites().size()) ? status.sites().get(row) : null;
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
            if (y > WarehouseControllerMenu.UPGRADES_Y - 22) {
                break;
            }
            graphics.text(font, line, WarehouseControllerMenu.STATUS_X, y, colour, false);
            y += 9;
        }
        return y + 1;
    }
}
