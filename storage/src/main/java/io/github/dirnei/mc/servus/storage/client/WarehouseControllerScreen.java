package io.github.dirnei.mc.servus.storage.client;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.input.KeyEvent;
import io.github.dirnei.mc.servus.storage.controller.RenameSitePayload;
import io.github.dirnei.mc.servus.storage.controller.SiteStatusPayload;
import io.github.dirnei.mc.servus.storage.menu.WarehouseControllerMenu;
import io.github.dirnei.mc.servus.storage.site.ChunkMapLayout;
import io.github.dirnei.mc.servus.storage.site.SiteCapacity;
import io.github.dirnei.mc.servus.storage.site.SiteName;
import io.github.dirnei.mc.servus.storage.terminal.TerminalLayout.Rect;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/** The site GUI of a Warehouse Controller: name, status, chunk map and Chunk Loader Upgrades. */
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
    /** The side panel with the team's other sites, right of the GUI. */
    private static final int SITES_GAP = 2;
    private static final int SITES_WIDTH = 120;
    private static final int SITE_ROW = 11;
    private static final int SITES_MAX = 16;

    private EditBox name;
    private boolean nameWasFocused;
    private String sentName;
    /** Where the online line was drawn, for its summary tooltip. */
    private int onlineLineY = -1;

    public WarehouseControllerScreen(WarehouseControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, WarehouseControllerMenu.WIDTH, WarehouseControllerMenu.IMAGE_HEIGHT);
        ClientSiteStatus.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = WarehouseControllerMenu.INVENTORY_Y - 11;
        // The name is the title: a borderless field in the title row, showing the default name until one is typed.
        name = new EditBox(font, leftPos + titleLabelX, topPos + titleLabelY, imageWidth - 2 * titleLabelX, 10,
                Component.translatable("gui.servus_storage.controller.name"));
        name.setMaxLength(SiteName.MAX_LENGTH);
        name.setHint(Component.literal(SiteName.display("", menu.pos().getX(), menu.pos().getZ())));
        name.setBordered(false);
        name.setTextColor(COLOUR_TEXT);
        name.setTextShadow(false);
        name.setTooltip(Tooltip.create(Component.translatable("gui.servus_storage.controller.name_hint")));
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
        int scanCode = event.scancode();
        int modifiers = event.modifiers();
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

    private static Rect map() {
        return new Rect(WarehouseControllerMenu.MAP_X, WarehouseControllerMenu.MAP_Y, WarehouseControllerMenu.MAP_SIZE, WarehouseControllerMenu.MAP_SIZE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        SiteStatusPayload status = ClientSiteStatus.status();
        Rect row = upgradesRow();
        Rect map = map();
        boolean overRow = x >= row.x() - 1 && x < row.x() + row.w() + 1 && y >= WarehouseControllerMenu.UPGRADES_Y - 12 && y < row.y() + row.h();
        if (menu.getCarried().isEmpty() && overRow && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.servus_storage.controller.upgrades.tooltip"), 180), mouseX, mouseY);
        } else if (x >= map.x() && x < map.right() && y >= map.y() && y < map.bottom()) {
            graphics.setTooltipForNextFrame(font, font.split(Component.translatable("gui.servus_storage.controller.map.tooltip"), 180), mouseX, mouseY);
        } else if (siteUnder(mouseX, mouseY, status) != null) {
            io.github.dirnei.mc.servus.storage.terminal.SiteRow site = siteUnder(mouseX, mouseY, status);
            // Name and state only.
            List<Component> lines = List.of(Component.literal(site.name()),
                    Component.translatable("gui.servus_storage.site.state." + site.remoteState().name().toLowerCase(java.util.Locale.ROOT)));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else if (onlineLineY >= 0 && x >= WarehouseControllerMenu.STATUS_X && y >= onlineLineY && y < onlineLineY + 9
                && !status.summary().getString().isEmpty()) {
            graphics.setTooltipForNextFrame(font, font.split(status.summary(), 200), mouseX, mouseY);
        } else {
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        if (menu.upgradeSlots() > 0) {
            PanelPainter.slots(graphics, leftPos, topPos, upgradesRow(), menu.upgradeSlots(), menu.upgradeSlots());
        }
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, WarehouseControllerMenu.INVENTORY_Y, 9 * 18, 3 * 18), 9, 27);
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, menu.hotbarY(), 9 * 18, 18), 9, 9);
        renderMap(graphics, ClientSiteStatus.status());
        renderSites(graphics, ClientSiteStatus.status());
    }

    /** The site panel right of the GUI, so JEI keeps its item list clear of it. */
    public List<net.minecraft.client.renderer.Rect2i> extraAreas() {
        SiteStatusPayload status = ClientSiteStatus.status();
        if (status == SiteStatusPayload.EMPTY) {
            return List.of();
        }
        int rows = Math.max(1, Math.min(SITES_MAX, status.sites().size()));
        return List.of(new net.minecraft.client.renderer.Rect2i(sitesLeft(), topPos, SITES_WIDTH, 18 + rows * SITE_ROW + 4));
    }

    /** A click on the site panel is not a click outside the GUI, so a carried stack is not dropped. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        boolean onPanel = extraAreas().stream().anyMatch(area -> mouseX >= area.getX() && mouseX < area.getX() + area.getWidth()
                && mouseY >= area.getY() && mouseY < area.getY() + area.getHeight());
        return !onPanel && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    private int sitesLeft() {
        return leftPos + imageWidth + SITES_GAP;
    }

    /** The side panel: a title, then one row per site with a state mark and its name, orange when it cannot trade. */
    private void renderSites(GuiGraphicsExtractor graphics, SiteStatusPayload status) {
        if (status == SiteStatusPayload.EMPTY) {
            return;
        }
        int rows = Math.max(1, Math.min(SITES_MAX, status.sites().size()));
        int left = sitesLeft();
        PanelPainter.panel(graphics, left, topPos, SITES_WIDTH, 18 + rows * SITE_ROW + 4);
        graphics.text(font, Component.translatable("gui.servus_storage.controller.sites"), left + 6, topPos + 6, COLOUR_TEXT, false);
        if (status.sites().isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_storage.controller.sites.none"), left + 6, topPos + 19, COLOUR_DIM, false);
            return;
        }
        for (int i = 0; i < rows; i++) {
            io.github.dirnei.mc.servus.storage.terminal.SiteRow site = status.sites().get(i);
            int y = topPos + 18 + i * SITE_ROW;
            int mark = switch (site.remoteState()) {
                case ONLINE -> 0xFF40B040;
                case SLEEPING -> 0xFFE0B040;
                case UNREACHABLE -> 0xFFC03030;
            };
            graphics.fill(left + 6, y + 1, left + 12, y + 7, mark);
            String name = font.plainSubstrByWidth(site.name(), SITES_WIDTH - 21);
            // A site whose controller is not wireless capable cannot trade; its name shows in orange.
            graphics.text(font, name, left + 15, y, site.trade() ? COLOUR_TEXT : COLOUR_NO_TRADE, false);
        }
    }

    private io.github.dirnei.mc.servus.storage.terminal.SiteRow siteUnder(int mouseX, int mouseY, SiteStatusPayload status) {
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
        Component upgrades = Component.translatable("gui.servus_storage.controller.upgrades");
        graphics.text(font, upgrades, (imageWidth - font.width(upgrades)) / 2, WarehouseControllerMenu.UPGRADES_Y - 11, COLOUR_TEXT, false);
        onlineLineY = -1;
        if (status == SiteStatusPayload.EMPTY) {
            return;
        }
        int y = WarehouseControllerMenu.MAP_Y;
        Component online = switch (status.online()) {
            case 0 -> Component.translatable("gui.servus_storage.controller.online");
            case 2 -> Component.translatable("gui.servus_storage.controller.offline_multiple");
            default -> Component.translatable("gui.servus_storage.controller.offline_none");
        };
        onlineLineY = y;
        y = lines(graphics, online, y, status.online() == 0 ? COLOUR_GOOD : COLOUR_BAD);
        y = lines(graphics, Component.translatable("gui.servus_storage.controller.chunks", status.chunks().size()), y, COLOUR_TEXT);
        switch (status.capacityState()) {
            case COVERED -> y = lines(graphics, Component.translatable("gui.servus_storage.controller.covered",
                    status.chunks().size(), status.capacity()), y, COLOUR_GOOD);
            case OVER -> y = lines(graphics, Component.translatable("gui.servus_storage.controller.over",
                    status.chunks().size(), status.capacity()), y, COLOUR_BAD);
            // The empty upgrade slots say it on their own.
            case NO_UPGRADE -> {
            }
        }
        if (!status.wake().getString().isEmpty()) {
            lines(graphics, status.wake(), y, status.wakeAlert() ? COLOUR_BAD : COLOUR_TEXT);
        }
    }

    /** Draws a wrapped text at the status column; returns the y below it. */
    private int lines(GuiGraphicsExtractor graphics, Component text, int y, int colour) {
        List<FormattedCharSequence> wrapped = new ArrayList<>(font.split(text, STATUS_WIDTH));
        for (FormattedCharSequence line : wrapped) {
            if (y > WarehouseControllerMenu.UPGRADES_Y - 22) {
                break;
            }
            graphics.text(font, line, WarehouseControllerMenu.STATUS_X, y, colour, false);
            y += 9;
        }
        return y + 1;
    }
}
