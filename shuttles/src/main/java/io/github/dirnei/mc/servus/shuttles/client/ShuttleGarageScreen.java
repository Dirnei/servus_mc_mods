package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.ShuttleGarageMenu;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A centred row of shuttle slots, a count line, the player inventory. */
public class ShuttleGarageScreen extends AbstractContainerScreen<ShuttleGarageMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int STATUS_Y = 52;
    private static final int BUTTONS_Y = 64;
    private static final int COLOUR_TEXT = 0xFF404040;
    private static final int COLOUR_LOCKED = 0x80FF9A2E;
    private static final int COLOUR_ALERT = 0xFFB03030;
    private static final int COLOUR_STRANDED = 0x80B03030;
    private static final int STRANDED_Y = 18;
    private static final int LINE_WIDTH = 160;

    private Button toggle;

    public ShuttleGarageScreen(ShuttleGarageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 178);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleGarageMenu.INVENTORY_Y - 11;
        addRenderableWidget(Button.builder(Component.translatable("gui.servus_shuttles.garage.recall"),
                        button -> MenuButtons.press(minecraft, menu, ShuttleGarageMenu.BUTTON_RECALL))
                .bounds(leftPos + 8, topPos + BUTTONS_Y, 76, 14).build());
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> MenuButtons.press(minecraft, menu, ShuttleGarageMenu.BUTTON_TOGGLE))
                .bounds(leftPos + 92, topPos + BUTTONS_Y, 76, 14).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.isActive() ? "gui.servus_shuttles.garage.active" : "gui.servus_shuttles.garage.paused"));
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
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        for (int slot = 0; slot < menu.capacity(); slot++) {
            if (menu.stranded(slot)) {
                int x = menu.slotX(slot);
                graphics.fill(x, ShuttleGarageMenu.SLOTS_Y, x + 16, ShuttleGarageMenu.SLOTS_Y + 16, COLOUR_STRANDED);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        List<Component> stranded = menu.strandedLines();
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (!stranded.isEmpty() && x >= 8 && x < 8 + LINE_WIDTH && y >= STRANDED_Y && y < STRANDED_Y + 9) {
            graphics.setComponentTooltipForNextFrame(font, stranded, mouseX, mouseY);
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int count = menu.garage().shuttleCount();
        Component line = Component.translatable(menu.isActive() ? "gui.servus_shuttles.garage.status" : "gui.servus_shuttles.garage.status_paused",
                count, menu.capacity(), menu.out());
        graphics.text(font, line, 8, STATUS_Y, COLOUR_TEXT, false);
        List<Component> stranded = menu.strandedLines();
        if (!stranded.isEmpty()) {
            graphics.text(font, Language.getInstance().getVisualOrder(font.substrByWidth(stranded.get(0), LINE_WIDTH)), 8, STRANDED_Y, COLOUR_ALERT, false);
        }
        for (int slot = 0; slot < menu.capacity(); slot++) {
            if (menu.locked(slot)) {
                int x = menu.slotX(slot);
                graphics.fill(x, ShuttleGarageMenu.SLOTS_Y, x + 16, ShuttleGarageMenu.SLOTS_Y + 16, COLOUR_LOCKED);
            }
        }
    }
}
