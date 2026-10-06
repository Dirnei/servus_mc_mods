package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.DispatchStationMenu;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload.ZoneRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Buffer row, read only zone overview with hover details, station state, player inventory. */
public class DispatchStationScreen extends AbstractContainerScreen<DispatchStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/dispatch_station.png");
    private static final int LIST_X = 8;
    private static final int LIST_Y = 40;
    private static final int LIST_WIDTH = 160;
    private static final int ROW_HEIGHT = 10;
    private static final int ROWS = 6;
    private static final int STATUS_Y = 104;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_GREY = 0x8A8A8A;
    private static final int COLOUR_ALERT = 0xB03030;

    private int scrollRow;

    public DispatchStationScreen(DispatchStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        ClientZoneRules.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = DispatchStationMenu.INVENTORY_Y - 11;
    }

    private ZoneRulesSyncPayload state() {
        return ClientZoneRules.state();
    }

    private int rowAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - LIST_X;
        int y = (int) mouseY - topPos - LIST_Y;
        if (x < 0 || y < 0 || x >= LIST_WIDTH || y >= ROWS * ROW_HEIGHT) {
            return -1;
        }
        return y / ROW_HEIGHT + scrollRow;
    }

    private Optional<ZoneRow> zoneAt(double mouseX, double mouseY) {
        int row = rowAt(mouseX, mouseY);
        List<ZoneRow> zones = state().zones();
        return row >= 0 && row < zones.size() && state().online() ? Optional.of(zones.get(row)) : Optional.empty();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        zoneAt(mouseX, mouseY).ifPresentOrElse(zone -> {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(zone.display()));
            if (zone.items().isEmpty() && zone.tags().isEmpty()) {
                lines.add(Component.translatable("gui.servus_storage.station.takes_all"));
            }
            if (!zone.items().isEmpty()) {
                lines.add(Component.translatable("gui.servus_storage.station.items", String.join(", ", zone.items())));
            }
            if (!zone.tags().isEmpty()) {
                lines.add(Component.translatable("gui.servus_storage.station.tags", String.join(", ", zone.tags())));
            }
            graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
        }, () -> renderTooltip(graphics, mouseX, mouseY));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int hovered = rowAt(mouseX, mouseY);
        if (hovered >= 0 && state().online() && hovered < state().zones().size()) {
            int y = topPos + LIST_Y + (hovered - scrollRow) * ROW_HEIGHT;
            graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT, 0x30FFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        ZoneRulesSyncPayload state = state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + state.reason());
            graphics.drawCenteredString(font, reason, imageWidth / 2, LIST_Y + 24, COLOUR_ALERT);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.station.zones"), LIST_X, LIST_Y - 9, COLOUR_TEXT, false);
        List<ZoneRow> zones = state.zones();
        if (zones.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.station.no_zones"), LIST_X + 2, LIST_Y + 1, COLOUR_GREY, false);
        }
        for (int i = 0; i < ROWS; i++) {
            int index = scrollRow + i;
            if (index >= zones.size()) {
                break;
            }
            ZoneRow row = zones.get(index);
            int y = LIST_Y + i * ROW_HEIGHT + 1;
            graphics.drawString(font, row.display(), LIST_X + 2, y, COLOUR_TEXT, false);
            String right = Component.translatable("gui.servus_storage.station.blocks", row.blocks()).getString();
            graphics.drawString(font, right, LIST_X + LIST_WIDTH - 2 - font.width(right), y, COLOUR_TEXT, false);
        }
        graphics.drawString(font, status(state), LIST_X, STATUS_Y, state.blockedReason().isEmpty() ? COLOUR_TEXT : COLOUR_ALERT, false);
    }

    private Component status(ZoneRulesSyncPayload state) {
        if (state.blockedReason().isEmpty()) {
            return Component.translatable(state.idle() ? "gui.servus_storage.station.idle" : "gui.servus_storage.station.busy");
        }
        String item = state.blockedStack().getHoverName().getString();
        String reason = state.blockedReason().toLowerCase(Locale.ROOT);
        if (reason.equals("no_room") && state.blockedZone().isEmpty()) {
            return Component.translatable("gui.servus_storage.station.blocked.no_room_general", item);
        }
        return Component.translatable("gui.servus_storage.station.blocked." + reason, item, state.blockedZone());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + ROWS * ROW_HEIGHT) {
            int maxRow = Math.max(0, state().zones().size() - ROWS);
            scrollRow = Math.max(0, Math.min(maxRow, scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        super.removed();
        ClientZoneRules.clear();
    }
}
