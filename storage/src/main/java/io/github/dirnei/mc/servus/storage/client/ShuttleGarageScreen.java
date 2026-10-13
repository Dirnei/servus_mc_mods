package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ShuttleGarageMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A centred row of shuttle slots, a count line, the player inventory. */
public class ShuttleGarageScreen extends AbstractContainerScreen<ShuttleGarageMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int STATUS_Y = 52;
    private static final int BUTTONS_Y = 64;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_LOCKED = 0x80FF9A2E;

    private Button toggle;

    public ShuttleGarageScreen(ShuttleGarageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 178);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleGarageMenu.INVENTORY_Y - 11;
        addRenderableWidget(Button.builder(Component.translatable("gui.servus_storage.garage.recall"),
                        button -> MenuButtons.press(minecraft, menu, ShuttleGarageMenu.BUTTON_RECALL))
                .bounds(leftPos + 8, topPos + BUTTONS_Y, 76, 14).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> MenuButtons.press(minecraft, menu, ShuttleGarageMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 92, topPos + BUTTONS_Y, 76, 14).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.isActive() ? "gui.servus_storage.garage.active" : "gui.servus_storage.garage.paused"));
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            PanelPainter.sunken(graphics, leftPos + menu.slotX(slot) - 1, topPos + ShuttleGarageMenu.SLOTS_Y - 1, 18, 18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int count = menu.garage().shuttleCount();
        Component line = Component.translatable(menu.isActive() ? "gui.servus_storage.garage.status" : "gui.servus_storage.garage.status_paused",
                count, menu.capacity(), menu.out());
        graphics.text(font, line, 8, STATUS_Y, COLOUR_TEXT, false);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            if (menu.locked(slot)) {
                int x = menu.slotX(slot);
                graphics.fill(x, ShuttleGarageMenu.SLOTS_Y, x + 16, ShuttleGarageMenu.SLOTS_Y + 16, COLOUR_LOCKED);
            }
        }
    }
}
