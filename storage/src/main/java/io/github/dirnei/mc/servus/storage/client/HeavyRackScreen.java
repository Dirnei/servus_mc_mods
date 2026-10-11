package io.github.dirnei.mc.servus.storage.client;

import net.minecraft.client.renderer.RenderPipelines;
import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Chest style screen for a rack section, drawn with the vanilla generic
 * container texture: one slot row strip per row, so it grows past six rows.
 */
public class HeavyRackScreen extends AbstractContainerScreen<HeavyRackMenu> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROW_HEIGHT = 18;
    private static final int TOP_HEIGHT = 17;
    private static final int BOTTOM_HEIGHT = 96;

    private final int rows;

    public HeavyRackScreen(HeavyRackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, TOP_HEIGHT + menu.rows() * ROW_HEIGHT + BOTTOM_HEIGHT);
        this.rows = menu.rows();
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // Header, then the texture's first slot row once per row, then the player inventory part.
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, TOP_HEIGHT, 256, 256);
        for (int row = 0; row < rows; row++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + TOP_HEIGHT + row * ROW_HEIGHT, 0, TOP_HEIGHT, imageWidth, ROW_HEIGHT, 256, 256);
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos + TOP_HEIGHT + rows * ROW_HEIGHT, 0, 126, imageWidth, BOTTOM_HEIGHT, 256, 256);
    }
}
