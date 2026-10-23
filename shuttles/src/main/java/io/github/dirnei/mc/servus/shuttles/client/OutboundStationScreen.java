package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.shuttles.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.shuttles.station.StationStatePayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Keep row, buffer row, state line, player inventory, the shared side panel on the left. */
public class OutboundStationScreen extends AbstractContainerScreen<OutboundStationMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/outbound_station.png");
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_ALERT = 0xFFB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == RelativeSide.FRONT,
            side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(side)));

    public OutboundStationScreen(OutboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = OutboundStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
    }

    private boolean isKeepSlot(Slot slot) {
        return slot != null && slot.index < menu.keepSlots();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (isKeepSlot(hoveredSlot)) {
            ItemStack keep = hoveredSlot.getItem();
            List<Component> lines = keep.isEmpty()
                    ? List.of(Component.translatable("gui.servus_shuttles.outbound.keep_hint"))
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        panel.render(graphics, font, mouseX, mouseY);
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }

    /** Scrolling over a keep slot adjusts its amount by one, by a stack with shift. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = hoveredSlot;
        if (isKeepSlot(slot) && !slot.getItem().isEmpty()) {
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

    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        LockOverlay.draw(graphics, menu, menu.locks());
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!LockOverlay.tooltip(graphics, font, hoveredSlot, menu.locks(), mouseX, mouseY)) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }
}
