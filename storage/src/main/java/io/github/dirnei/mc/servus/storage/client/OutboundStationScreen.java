package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.OutboundStationMenu;
import io.github.dirnei.mc.servus.storage.outbound.KeepSlotPayload;
import io.github.dirnei.mc.servus.storage.station.StationStatePayload;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Keep row, buffer row, state line, player inventory, the shared side panel on the left. */
public class OutboundStationScreen extends AbstractContainerScreen<OutboundStationMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/outbound_station.png");
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_ALERT = 0xB03030;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == io.github.dirnei.mc.servus.foundation.side.RelativeSide.FRONT, side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

    public OutboundStationScreen(OutboundStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        ClientStationState.clear();
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = OutboundStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Slot hovered = hoveredSlot;
        if (hovered != null && hovered.index < menu.keepSlots()) {
            ItemStack keep = hovered.getItem();
            List<Component> lines = keep.isEmpty()
                    ? List.of(Component.translatable("gui.servus_storage.outbound.keep_hint"))
                    : List.of(Component.translatable("gui.servus_storage.outbound.keep_line", keep.getCount(), keep.getHoverName()),
                            Component.translatable("gui.servus_storage.outbound.keep_adjust"));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (panel.renderTooltip(graphics, font, mouseX, mouseY)) {
            return;
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(graphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.outbound.keep"), 8, OutboundStationMenu.KEEP_Y - 10, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.outbound.buffer"), 8, OutboundStationMenu.BUFFER_Y - 10, COLOUR_TEXT, false);
        StationStatePayload state = ClientStationState.state();
        if (!state.online()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.terminal." + state.reason()), 8, OutboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
            return;
        }
        if (!state.blockedReason().isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.outbound.short." + state.blockedReason(), state.blockedStack().getHoverName()),
                    8, OutboundStationMenu.STATUS_Y, COLOUR_ALERT, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Scrolling over a keep slot adjusts its amount by one, by a stack with shift. */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Slot slot = hoveredSlot;
        if (slot != null && slot.index < menu.keepSlots() && !slot.getItem().isEmpty()) {
            ItemStack keep = slot.getItem();
            int step = hasShiftDown() ? keep.getMaxStackSize() : 1;
            int amount = Math.max(1, keep.getCount() + (scrollY > 0 ? step : -step));
            PacketDistributor.sendToServer(new KeepSlotPayload(slot.index, keep.copyWithCount(1), amount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void removed() {
        super.removed();
        ClientStationState.clear();
    }
}
