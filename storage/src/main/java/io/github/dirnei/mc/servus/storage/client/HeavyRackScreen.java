package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.menu.HeavyRackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Chest style screen drawn with the vanilla generic container texture, cropped to the rack's rows. */
public class HeavyRackScreen extends AbstractContainerScreen<HeavyRackMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int ROW_HEIGHT = 18;
    private static final int TOP_HEIGHT = 17;
    private static final int BOTTOM_HEIGHT = 96;

    private final int rows;

    public HeavyRackScreen(HeavyRackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.rows = menu.rows();
        this.imageWidth = 176;
        this.imageHeight = TOP_HEIGHT + rows * ROW_HEIGHT + BOTTOM_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int top = TOP_HEIGHT + rows * ROW_HEIGHT;
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, top);
        graphics.blit(TEXTURE, leftPos, topPos + top, 0, 126, imageWidth, BOTTOM_HEIGHT);
    }
}
