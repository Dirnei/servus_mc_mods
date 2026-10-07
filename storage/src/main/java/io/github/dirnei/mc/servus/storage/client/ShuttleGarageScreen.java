package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** A centred row of shuttle slots, a count line, the player inventory. */
public class ShuttleGarageScreen extends AbstractContainerScreen<ShuttleGarageMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int STATUS_Y = 52;
    private static final int BUTTONS_Y = 64;

    private Button toggle;
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_LOCKED = 0x80FF9A2E;

    public ShuttleGarageScreen(ShuttleGarageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleGarageMenu.INVENTORY_Y - 11;
        addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.garage.recall"), button -> press(ShuttleGarageMenu.BUTTON_RECALL))
                .bounds(leftPos + 8, topPos + BUTTONS_Y, 76, 14).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> press(ShuttleGarageMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 92, topPos + BUTTONS_Y, 76, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.isActive() ? "gui.servus_storage.garage.active" : "gui.servus_storage.garage.paused"));
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            int x = leftPos + menu.slotX(slot) - 1;
            int y = topPos + ShuttleGarageMenu.SLOTS_Y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
            graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
            graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
            graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
            graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int count = menu.garage().shuttleCount();
        Component line = Component.translatable(menu.isActive() ? "gui.servus_storage.garage.status" : "gui.servus_storage.garage.status_paused",
                count, menu.capacity(), menu.out());
        graphics.drawString(font, line, 8, STATUS_Y, COLOUR_TEXT, false);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            if (menu.locked(slot)) {
                int x = menu.slotX(slot);
                graphics.fill(x, ShuttleGarageMenu.SLOTS_Y, x + 16, ShuttleGarageMenu.SLOTS_Y + 16, COLOUR_LOCKED);
            }
        }
    }
}
