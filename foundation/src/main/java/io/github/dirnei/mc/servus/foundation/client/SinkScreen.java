package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.menu.SinkMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
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
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (menu.getCarried().isEmpty() && isHovering(SinkMenu.TRASH_X, SinkMenu.TRASH_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font, Component.translatable("gui.servus_foundation.sink.trash"), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractLabels(guiGraphics, mouseX, mouseY);
        line(guiGraphics, 0, Component.translatable("gui.servus_foundation.sink.height", menu.chimneyHeight()));
        line(guiGraphics, 1, Component.translatable("gui.servus_foundation.sink.items", rate(menu.itemRate(), "items")));
        line(guiGraphics, 2, Component.translatable("gui.servus_foundation.sink.fluid", rate(menu.fluidRate(), "fluid")));
        line(guiGraphics, 3, Component.translatable("gui.servus_foundation.sink.energy", rate(menu.energyRate(), "energy")));
        guiGraphics.text(font, Component.translatable("gui.servus_foundation.sink.hint"), LINE_X, LINE_Y + 4 * LINE_HEIGHT + 2,
                HINT, false);
    }

    private void line(GuiGraphicsExtractor guiGraphics, int index, Component text) {
        guiGraphics.text(font, text, LINE_X, LINE_Y + index * LINE_HEIGHT, TEXT, false);
    }

    /** "16 per second", or Unlimited for -1. */
    private static Component rate(int value, String unit) {
        if (value < 0) {
            return Component.translatable("gui.servus_foundation.sink.unlimited");
        }
        return Component.translatable("gui.servus_foundation.sink.unit." + unit, value);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
