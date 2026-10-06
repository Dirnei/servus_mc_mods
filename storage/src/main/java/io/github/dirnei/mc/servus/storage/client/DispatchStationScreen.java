package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.DispatchStationMenu;
import io.github.dirnei.mc.servus.storage.station.SelectZonePayload;
import io.github.dirnei.mc.servus.storage.station.SetOverflowPayload;
import io.github.dirnei.mc.servus.storage.station.SetZoneTagsPayload;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload.ZoneRow;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Buffer row, zone list, the selected zone's rule editor, player inventory. */
public class DispatchStationScreen extends AbstractContainerScreen<DispatchStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/dispatch_station.png");
    private static final int LIST_X = 8;
    private static final int LIST_Y = 40;
    private static final int LIST_WIDTH = 160;
    private static final int ROW_HEIGHT = 10;
    private static final int ROWS = 4;
    private static final int RULE_LABEL_Y = 82;
    private static final int TAGS_Y = 114;
    private static final int STATUS_Y = 130;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_GREY = 0x8A8A8A;
    private static final int COLOUR_ALERT = 0xB03030;

    private EditBox tags;
    private Button overflow;
    private int scrollRow;
    private int seenVersion = -1;
    private String seenSelection = "";
    private boolean tagsWereFocused;

    public DispatchStationScreen(DispatchStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 232;
        ClientZoneRules.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = DispatchStationMenu.INVENTORY_Y - 11;
        tags = new EditBox(font, leftPos + 8, topPos + TAGS_Y, 112, 12, Component.translatable("gui.servus_storage.station.tags"));
        tags.setMaxLength(200);
        tags.setHint(Component.translatable("gui.servus_storage.station.tags_hint"));
        tags.setBordered(true);
        addRenderableWidget(tags);
        overflow = Button.builder(Component.translatable("gui.servus_storage.station.overflow_off"), button -> toggleOverflow())
                .bounds(leftPos + 124, topPos + TAGS_Y - 1, 44, 14).build();
        addRenderableWidget(overflow);
        applyState();
    }

    private ZoneRulesSyncPayload state() {
        return ClientZoneRules.state();
    }

    /** Pulls the server state into the widgets when it changed. */
    private void applyState() {
        if (seenVersion == ClientZoneRules.version()) {
            return;
        }
        seenVersion = ClientZoneRules.version();
        ZoneRulesSyncPayload state = state();
        boolean selectionChanged = !state.selected().equals(seenSelection);
        seenSelection = state.selected();
        if (selectionChanged || !tags.isFocused()) {
            tags.setValue(state.tagLine());
        }
        boolean editable = state.online() && !state.selected().isEmpty();
        tags.setEditable(editable);
        overflow.active = editable;
        overflow.setMessage(Component.translatable(state.overflow()
                ? "gui.servus_storage.station.overflow_on" : "gui.servus_storage.station.overflow_off"));
    }

    private void toggleOverflow() {
        PacketDistributor.sendToServer(new SetOverflowPayload(!state().overflow()));
    }

    private void sendTags() {
        if (!state().selected().isEmpty() && !tags.getValue().equals(state().tagLine())) {
            PacketDistributor.sendToServer(new SetZoneTagsPayload(tags.getValue()));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        applyState();
        boolean focused = tags.isFocused();
        if (tagsWereFocused && !focused) {
            sendTags();
        }
        tagsWereFocused = focused;
    }

    // Zone list

    private int rowAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - LIST_X;
        int y = (int) mouseY - topPos - LIST_Y;
        if (x < 0 || y < 0 || x >= LIST_WIDTH || y >= ROWS * ROW_HEIGHT) {
            return -1;
        }
        return y / ROW_HEIGHT + scrollRow;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        ZoneRulesSyncPayload state = state();
        if (!state.online()) {
            return;
        }
        List<ZoneRow> zones = state.zones();
        int hovered = rowAt(mouseX, mouseY);
        for (int i = 0; i < ROWS; i++) {
            int index = scrollRow + i;
            if (index >= zones.size()) {
                break;
            }
            int y = topPos + LIST_Y + i * ROW_HEIGHT;
            if (zones.get(index).key().equals(state.selected())) {
                graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT, 0x60FFFFFF);
            } else if (index == hovered) {
                graphics.fill(leftPos + LIST_X, y, leftPos + LIST_X + LIST_WIDTH, y + ROW_HEIGHT, 0x30FFFFFF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        ZoneRulesSyncPayload state = state();
        if (!state.online()) {
            Component reason = Component.translatable("gui.servus_storage.terminal." + state.reason());
            graphics.drawCenteredString(font, reason, imageWidth / 2, LIST_Y + 14, COLOUR_ALERT);
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
            int colour = row.blocks() == 0 ? COLOUR_GREY : COLOUR_TEXT;
            String name = row.display() + (row.overflow() ? " *" : "");
            graphics.drawString(font, name, LIST_X + 2, y, colour, false);
            String right = Component.translatable("gui.servus_storage.station.blocks", row.blocks()).getString();
            graphics.drawString(font, right, LIST_X + LIST_WIDTH - 2 - font.width(right), y, colour, false);
        }
        Component rule = state.selected().isEmpty()
                ? Component.translatable("gui.servus_storage.station.no_zone")
                : Component.translatable("gui.servus_storage.station.rule", displayOf(state, state.selected()));
        graphics.drawString(font, rule, LIST_X, RULE_LABEL_Y, COLOUR_TEXT, false);
        graphics.drawString(font, status(state), LIST_X, STATUS_Y, state.blockedReason().isEmpty() ? COLOUR_TEXT : COLOUR_ALERT, false);
    }

    private static String displayOf(ZoneRulesSyncPayload state, String key) {
        for (ZoneRow row : state.zones()) {
            if (row.key().equals(key)) {
                return row.display();
            }
        }
        return key;
    }

    private Component status(ZoneRulesSyncPayload state) {
        if (state.blockedReason().isEmpty()) {
            return Component.translatable(state.idle() ? "gui.servus_storage.station.idle" : "gui.servus_storage.station.busy");
        }
        String item = state.blockedStack().getHoverName().getString();
        String zone = state.blockedZone().isEmpty() ? Component.translatable("gui.servus_storage.station.general").getString() : state.blockedZone();
        return Component.translatable("gui.servus_storage.station.blocked." + state.blockedReason().toLowerCase(Locale.ROOT), item, zone);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        ZoneRulesSyncPayload state = state();
        if (row >= 0 && state.online() && row < state.zones().size()) {
            sendTags();
            PacketDistributor.sendToServer(new SelectZonePayload(state.zones().get(row).key()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rowAt(mouseX, mouseY) >= 0 || (mouseY >= topPos + LIST_Y && mouseY < topPos + LIST_Y + ROWS * ROW_HEIGHT)) {
            int maxRow = Math.max(0, state().zones().size() - ROWS);
            scrollRow = Math.max(0, Math.min(maxRow, scrollRow - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (tags.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                sendTags();
                tags.setFocused(false);
                return true;
            }
            if (tags.keyPressed(keyCode, scanCode, modifiers) || tags.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        ClientZoneRules.clear();
    }
}
