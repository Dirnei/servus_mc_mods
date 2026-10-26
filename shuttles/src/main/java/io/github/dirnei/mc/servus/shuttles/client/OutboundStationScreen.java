package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.NamedMachineScreen;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.outbound.ExportListPayload;
import io.github.dirnei.mc.servus.shuttles.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import io.github.dirnei.mc.servus.shuttles.terminal.TerminalLayout.Rect;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Keep row with its exports, buffer row, state line, player inventory, the
 * shared side panel on the left, and the Claim an export picker over it all.
 */
public class OutboundStationScreen extends NamedMachineScreen<OutboundStationMenu> {

    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;

    private static final int COLOUR_EXPORT = 0xFFE07A1F;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == RelativeSide.FRONT,
            side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(side)));
    private final ExportPicker picker = new ExportPicker(menu::exportList);

    public OutboundStationScreen(OutboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, OutboundStationMenu.IMAGE_HEIGHT);
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = OutboundStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
        picker.build(font, leftPos, topPos, imageWidth, imageHeight).forEach(this::addRenderableWidget);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (picker.isOpen()) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                picker.close();
            } else {
                picker.search().keyPressed(event);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private boolean isKeepSlot(Slot slot) {
        return slot != null && slot.index < menu.keepSlots();
    }

    /** The export in a keep slot, or null. */
    private ExportListPayload.Slot exportIn(Slot slot) {
        return isKeepSlot(slot) ? menu.exportList().slot(slot.index) : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (picker.isOpen()) {
            picker.render(graphics, font, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
            return;
        }
        ExportListPayload.Slot export = exportIn(hoveredSlot);
        if (export != null) {
            List<Component> lines = List.of(Component.translatable("gui.servus_shuttles.outbound.export_line", export.item().getHoverName(),
                            export.threshold()),
                    Component.translatable("gui.servus_shuttles.outbound.export_state." + export.state()),
                    Component.translatable("gui.servus_shuttles.outbound.export_release"));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        if (isKeepSlot(hoveredSlot)) {
            ItemStack keep = hoveredSlot.getItem();
            List<Component> lines = keep.isEmpty()
                    ? List.of(Component.translatable("gui.servus_shuttles.outbound.keep_hint"),
                            Component.translatable("gui.servus_shuttles.outbound.claim_hint"))
                    : List.of(Component.translatable("gui.servus_shuttles.outbound.keep_line", keep.getCount(), keep.getHoverName()),
                            Component.translatable("gui.servus_shuttles.outbound.keep_adjust"));
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        panel.renderTooltip(graphics, font, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        PanelPainter.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        slotRow(graphics, OutboundStationMenu.KEEP_Y, menu.keepSlots());
        slotRow(graphics, OutboundStationMenu.BUFFER_Y, menu.bufferSlots());
        PanelPainter.playerInventory(graphics, leftPos, topPos, OutboundStationMenu.INVENTORY_Y, OutboundStationMenu.HOTBAR_Y);
        panel.render(graphics, font, mouseX, mouseY);
    }

    private void slotRow(GuiGraphicsExtractor graphics, int y, int count) {
        int columns = OutboundStationMenu.COLUMNS;
        int rows = (count + columns - 1) / columns;
        PanelPainter.slots(graphics, leftPos, topPos, new Rect(8, y, columns * 18, rows * 18), columns, count);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        graphics.text(font, Component.translatable("gui.servus_shuttles.outbound.keep"), 8, OutboundStationMenu.KEEP_Y - 10, COLOUR_TEXT, false);
        graphics.text(font, Component.translatable("gui.servus_shuttles.outbound.buffer"), 8, OutboundStationMenu.BUFFER_Y - 10, COLOUR_TEXT, false);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.terminal." + state.reason()), 8, OutboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        if (!state.blockedReason().isEmpty()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.outbound.short." + state.blockedReason(), state.blockedStack().getHoverName()),
                    8, OutboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
        }
    }

    /** An empty keep slot clicked with an empty hand opens the picker; while it is open, it takes every click. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (picker.isOpen()) {
            return picker.mouseClicked(event, doubleClick);
        }
        Slot slot = hoveredSlot;
        if (event.button() == 0 && isKeepSlot(slot) && !slot.hasItem() && exportIn(slot) == null && menu.getCarried().isEmpty()
                && ClientStationState.state().online()) {
            picker.open(slot.index);
            setFocused(picker.search());
            return true;
        }
        return panel.mouseClicked(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (picker.isOpen()) {
            picker.mouseDragged(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (picker.isOpen() && picker.mouseReleased()) {
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Scrolling over a keep slot adjusts its amount by one, by a stack with shift. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (picker.isOpen()) {
            picker.mouseScrolled(scrollY);
            return true;
        }
        Slot slot = hoveredSlot;
        if (isKeepSlot(slot) && !slot.getItem().isEmpty() && exportIn(slot) == null) {
            ItemStack keep = slot.getItem();
            int step = Minecraft.getInstance().hasShiftDown() ? keep.getMaxStackSize() : 1;
            int amount = Math.max(1, keep.getCount() + (scrollY > 0 ? step : -step));
            ClientPacketDistributor.sendToServer(new KeepSlotPayload(slot.index, keep.copyWithCount(1), amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }

    /** Draws every slot, then the exports in their keep slots: the item, an orange marker and the threshold. */
    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        LockOverlay.draw(graphics, menu, menu.locks());
        for (ExportListPayload.Slot export : menu.exportList().slots()) {
            if (export.slot() < 0 || export.slot() >= menu.keepSlots()) {
                continue;
            }
            Slot slot = menu.slots.get(export.slot());
            graphics.fakeItem(export.item(), slot.x, slot.y);
            graphics.fill(slot.x, slot.y, slot.x + 5, slot.y + 5, 0xFF202020);
            graphics.fill(slot.x + 1, slot.y + 1, slot.x + 4, slot.y + 4, COLOUR_EXPORT);
            Component above = Component.literal(StorageTerminalScreen.abbreviate(export.threshold()) + "+");
            PanelPainter.smallText(graphics, font, above, slot.x + 17, slot.y + 12, 0xFFFFFFFF);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (picker.isOpen()) {
            return;
        }
        if (!LockOverlay.tooltip(graphics, font, hoveredSlot, menu.locks(), mouseX, mouseY)) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }
}
