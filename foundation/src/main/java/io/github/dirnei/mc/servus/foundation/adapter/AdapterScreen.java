package io.github.dirnei.mc.servus.foundation.adapter;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Machine Adapter GUI: the faced machine, the upkeep ghost slot, the level
 * with minus and plus, and how many upkeep items the machine holds. Drawn
 * without a texture: a plain panel with slot frames.
 */
public class AdapterScreen extends AbstractContainerScreen<AdapterMenu> {

    private static final int TEXT = 0x404040;
    private static final int PANEL = 0xFFC6C6C6;
    private static final int DARK = 0xFF373737;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int SLOT = 0xFF8B8B8B;

    public AdapterScreen(AdapterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = AdapterMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        int y = topPos + AdapterMenu.GHOST_Y - 2;
        addRenderableWidget(Button.builder(Component.literal("-"), button -> click(AdapterMenu.BUTTON_LEVEL_DOWN))
                .bounds(leftPos + 104, y, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> click(AdapterMenu.BUTTON_LEVEL_UP))
                .bounds(leftPos + 148, y, 20, 20).build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x, y, x + imageWidth, y + 1, LIGHT);
        graphics.fill(x, y, x + 1, y + imageHeight, LIGHT);
        graphics.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, DARK);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, SLOT);
            graphics.fill(sx, sy, sx + 17, sy + 1, DARK);
            graphics.fill(sx, sy, sx + 1, sy + 17, DARK);
            graphics.fill(sx + 1, sy + 17, sx + 18, sy + 18, LIGHT);
            graphics.fill(sx + 17, sy + 1, sx + 18, sy + 18, LIGHT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component machine = menu.adapter().machineName();
        Component line = !menu.hasProfile()
                ? Component.translatable("gui.servus_foundation.machine_adapter.no_profile")
                : Component.translatable("gui.servus_foundation.machine_adapter.machine", machine);
        graphics.drawString(font, line, 8, 18, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_foundation.machine_adapter.upkeep"), 8, AdapterMenu.GHOST_Y - 10, TEXT, false);
        String level = Integer.toString(menu.level());
        graphics.drawString(font, level, 136 - font.width(level) / 2, AdapterMenu.GHOST_Y + 4, TEXT, false);
        ItemStack upkeep = menu.adapter().upkeepItem();
        Component count = upkeep.isEmpty()
                ? Component.translatable("gui.servus_foundation.machine_adapter.no_upkeep")
                : Component.translatable("gui.servus_foundation.machine_adapter.count", menu.count(), menu.level());
        graphics.drawString(font, count, 8, AdapterMenu.GHOST_Y + 22, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
