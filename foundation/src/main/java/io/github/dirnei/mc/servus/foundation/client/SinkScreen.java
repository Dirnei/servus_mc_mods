package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Sink GUI: the trash slot, the chimney height and the three rates. */
public class SinkScreen extends AbstractContainerScreen<SinkMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "textures/gui/sink.png");
    private static final int TEXT = 0xFF404040;
    private static final int HINT = 0xFF707070;
    private static final int LINE_X = 8;
    private static final int LINE_Y = 18;
    private static final int LINE_HEIGHT = 11;

    public SinkScreen(SinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && isHovering(SinkMenu.TRASH_X, SinkMenu.TRASH_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_foundation.sink.trash"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        line(guiGraphics, 0, Component.translatable("gui.servus_foundation.sink.height", menu.chimneyHeight()));
        line(guiGraphics, 1, Component.translatable("gui.servus_foundation.sink.items", rate(menu.itemRate(), "items")));
        line(guiGraphics, 2, Component.translatable("gui.servus_foundation.sink.fluid", rate(menu.fluidRate(), "fluid")));
        line(guiGraphics, 3, Component.translatable("gui.servus_foundation.sink.energy", rate(menu.energyRate(), "energy")));
        guiGraphics.drawString(font, Component.translatable("gui.servus_foundation.sink.hint"), LINE_X, LINE_Y + 4 * LINE_HEIGHT + 2,
                HINT, false);
    }

    private void line(GuiGraphics guiGraphics, int index, Component text) {
        guiGraphics.drawString(font, text, LINE_X, LINE_Y + index * LINE_HEIGHT, TEXT, false);
    }

    /** "16 per second", or Unlimited for -1. */
    private static Component rate(int value, String unit) {
        if (value < 0) {
            return Component.translatable("gui.servus_foundation.sink.unlimited");
        }
        return Component.translatable("gui.servus_foundation.sink.unit." + unit, value);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
