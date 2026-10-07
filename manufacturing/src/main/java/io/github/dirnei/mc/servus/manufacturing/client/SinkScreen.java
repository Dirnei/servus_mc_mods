package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.lib.block.SideConfigMenus;
import io.github.dirnei.mc.servus.lib.client.SideConfigPanel;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.menu.SinkMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Sink GUI: the trash slot, the chimney height, the three rates and the side panel. */
public class SinkScreen extends AbstractContainerScreen<SinkMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "textures/gui/sink.png");
    private static final int TEXT = 0xFF404040;
    private static final int HINT = 0xFF707070;
    private static final int LINE_X = 8;
    private static final int LINE_Y = 18;
    private static final int LINE_HEIGHT = 11;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> menu.sink().sideConfig().isLocked(side), side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

    public SinkScreen(SinkMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        panel.placeBeside(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (menu.getCarried().isEmpty() && isHovering(SinkMenu.TRASH_X, SinkMenu.TRASH_Y, 16, 16, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.sink.trash"), mouseX, mouseY);
        }
        panel.renderTooltip(guiGraphics, font, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        line(guiGraphics, 0, Component.translatable("gui.servus_manufacturing.sink.height", menu.chimneyHeight()));
        line(guiGraphics, 1, Component.translatable("gui.servus_manufacturing.sink.items", rate(menu.itemRate(), "items")));
        line(guiGraphics, 2, Component.translatable("gui.servus_manufacturing.sink.fluid", rate(menu.fluidRate(), "fluid")));
        line(guiGraphics, 3, Component.translatable("gui.servus_manufacturing.sink.energy", rate(menu.energyRate(), "energy")));
        guiGraphics.drawString(font, Component.translatable("gui.servus_manufacturing.sink.hint"), LINE_X, LINE_Y + 4 * LINE_HEIGHT + 2,
                HINT, false);
    }

    private void line(GuiGraphics guiGraphics, int index, Component text) {
        guiGraphics.drawString(font, text, LINE_X, LINE_Y + index * LINE_HEIGHT, TEXT, false);
    }

    /** "16 per second", or Unlimited for -1. */
    private static Component rate(int value, String unit) {
        if (value < 0) {
            return Component.translatable("gui.servus_manufacturing.sink.unlimited");
        }
        return Component.translatable("gui.servus_manufacturing.sink.unit." + unit, value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(guiGraphics, font, mouseX, mouseY);
    }
}
