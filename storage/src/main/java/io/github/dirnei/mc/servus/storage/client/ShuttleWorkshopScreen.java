package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.menu.ShuttleWorkshopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The shuttle slot, its tool and utility slots with labels, the player inventory. */
public class ShuttleWorkshopScreen extends AbstractContainerScreen<ShuttleWorkshopMenu> {

    /** The plain panel of the garage GUI; the slot frames are drawn here. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_CLOSED = 0x80373737;
    private static final int LABEL_Y = ShuttleWorkshopMenu.SLOTS_Y - 11;

    public ShuttleWorkshopScreen(ShuttleWorkshopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = ShuttleWorkshopMenu.INVENTORY_Y - 11;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        frame(graphics, ShuttleWorkshopMenu.SHUTTLE_X);
        frame(graphics, ShuttleWorkshopMenu.TOOL_X);
        frame(graphics, ShuttleWorkshopMenu.UTILITY_X);
    }

    private void frame(GuiGraphics graphics, int slotX) {
        int x = leftPos + slotX - 1;
        int y = topPos + ShuttleWorkshopMenu.SLOTS_Y - 1;
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 18, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 18, 0xFF373737);
        graphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.shuttle"), ShuttleWorkshopMenu.SHUTTLE_X - 4, LABEL_Y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.tool"), ShuttleWorkshopMenu.TOOL_X - 2, LABEL_Y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.utility"), ShuttleWorkshopMenu.UTILITY_X - 2, LABEL_Y, COLOUR_TEXT, false);
        if (menu.shuttle().isEmpty()) {
            // Module places are closed until a shuttle is in.
            for (int slotX : new int[] {ShuttleWorkshopMenu.TOOL_X, ShuttleWorkshopMenu.UTILITY_X}) {
                graphics.fill(slotX, ShuttleWorkshopMenu.SLOTS_Y, slotX + 16, ShuttleWorkshopMenu.SLOTS_Y + 16, COLOUR_CLOSED);
            }
            graphics.drawString(font, Component.translatable("gui.servus_storage.workshop.hint"), 8, ShuttleWorkshopMenu.SLOTS_Y + 24, COLOUR_TEXT, false);
        }
    }
}
