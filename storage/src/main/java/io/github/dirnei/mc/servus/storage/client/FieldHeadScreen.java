package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.ServusStorageIds;
import io.github.dirnei.mc.servus.storage.field.FieldStatus;
import io.github.dirnei.mc.servus.storage.menu.FieldHeadMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** The crop slot and harvest only toggle, the field's numbers, a water bar, the player inventory. */
public class FieldHeadScreen extends AbstractContainerScreen<FieldHeadMenu> {

    /** The plain panel of the garage GUI; slot frame and water bar are drawn here. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusStorageIds.MOD_ID, "textures/gui/shuttle_garage.png");
    private static final int COLOUR_TEXT = 0x404040;
    private static final int COLOUR_DUE = 0x2E7D32;
    private static final int COLOUR_WARN = 0xA04000;
    private static final int COLOUR_WATER = 0xFF3F76E4;
    private static final int BAR_X = 156;
    private static final int BAR_Y = 18;
    private static final int BAR_W = 12;
    private static final int BAR_H = 64;
    private static final int LINES_Y = 42;

    private Button toggle;

    public FieldHeadScreen(FieldHeadMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 178;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = FieldHeadMenu.INVENTORY_Y - 11;
        toggle = addRenderableWidget(Button.builder(Component.empty(), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, FieldHeadMenu.BUTTON_HARVEST_ONLY);
            }
        }).bounds(leftPos + 30, topPos + FieldHeadMenu.CROP_Y + 1, 110, 14).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.harvestOnly()
                ? "gui.servus_storage.field_head.harvest_only_on" : "gui.servus_storage.field_head.harvest_only_off"));
        super.render(graphics, mouseX, mouseY, partialTick);
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        if (mouseX >= barLeft && mouseX < barLeft + BAR_W && mouseY >= barTop && mouseY < barTop + BAR_H) {
            graphics.renderTooltip(font, Component.translatable("gui.servus_storage.field_head.water", menu.water(), menu.capacity()), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        sunken(graphics, leftPos + FieldHeadMenu.CROP_X - 1, topPos + FieldHeadMenu.CROP_Y - 1, 18, 18);
        int barLeft = leftPos + BAR_X;
        int barTop = topPos + BAR_Y;
        sunken(graphics, barLeft - 1, barTop - 1, BAR_W + 2, BAR_H + 2);
        int capacity = Math.max(1, menu.capacity());
        int filled = (int) ((long) BAR_H * Math.min(menu.water(), capacity) / capacity);
        if (filled > 0) {
            graphics.fill(barLeft, barTop + BAR_H - filled, barLeft + BAR_W, barTop + BAR_H, COLOUR_WATER);
        }
    }

    private static void sunken(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        graphics.fill(x, y, x + width, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + height, 0xFF373737);
        graphics.fill(x, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        int y = LINES_Y;
        FieldStatus status = menu.status();
        if (status != FieldStatus.SERVING) {
            graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.status." + status.name().toLowerCase(Locale.ROOT)),
                    8, y, COLOUR_WARN, false);
            return;
        }
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.size", menu.rails(), menu.plots()), 8, y, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable("gui.servus_storage.field_head.crops", menu.planted(), menu.ripe()), 8, y + 10, COLOUR_TEXT, false);
        graphics.drawString(font, Component.translatable(menu.hydrated() ? "gui.servus_storage.field_head.hydrated" : "gui.servus_storage.field_head.dry"),
                8, y + 20, menu.hydrated() ? COLOUR_TEXT : COLOUR_WARN, false);
        Component last;
        int colour = COLOUR_TEXT;
        if (!menu.harvestOnly() && menu.crop().isEmpty()) {
            last = Component.translatable("gui.servus_storage.field_head.no_crop");
        } else if (menu.harvestDue()) {
            last = Component.translatable("gui.servus_storage.field_head.due");
            colour = COLOUR_DUE;
        } else {
            last = Component.translatable("gui.servus_storage.field_head.growing");
        }
        graphics.drawString(font, last, 8, y + 30, colour, false);
    }
}
