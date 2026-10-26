package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.NamedMachineScreen;
import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.shuttles.ServusShuttlesIds;
import io.github.dirnei.mc.servus.shuttles.menu.CraftingStationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Crafting Station GUI: inputs on the left, the job the CPU handed the
 * station in the middle, a progress bar and the outputs on the right.
 */
public class CraftingStationScreen extends NamedMachineScreen<CraftingStationMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ServusShuttlesIds.MOD_ID, "textures/gui/crafting_station.png");
    private static final int CELL = 18;
    private static final int TEXT = 0xFF404040;
    private static final int GREEN = 0xFF60C060;

    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> side == RelativeSide.FRONT,
            side -> MenuButtons.press(minecraft, menu, SideConfigMenus.buttonOf(side)));

    public CraftingStationScreen(CraftingStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, CraftingStationMenu.WIDTH, CraftingStationMenu.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelX = CraftingStationMenu.INVENTORY_X;
        this.inventoryLabelY = CraftingStationMenu.INVENTORY_Y - 11;
        panel.placeBeside(leftPos, topPos);
    }

    private boolean inJobCell(double mouseX, double mouseY) {
        int x = leftPos + CraftingStationMenu.JOB_X;
        int y = topPos + CraftingStationMenu.JOB_Y;
        return mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ItemStack result = menu.station().clientJobResult();
        if (!result.isEmpty() && inJobCell(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, result, mouseX, mouseY);
        } else {
            panel.renderTooltip(graphics, font, mouseX, mouseY);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        panel.render(graphics, font, mouseX, mouseY);

        int px = leftPos + CraftingStationMenu.PROGRESS_X;
        int py = topPos + CraftingStationMenu.PROGRESS_Y;
        graphics.fill(px, py, px + 18, py + 2, 0xFF555555);
        if (menu.workTicks() > 0 && menu.progress() > 0) {
            int filled = Math.min(18, menu.progress() * 18 / menu.workTicks());
            graphics.fill(px, py, px + filled, py + 2, GREEN);
        }

        int jx = leftPos + CraftingStationMenu.JOB_X;
        int jy = topPos + CraftingStationMenu.JOB_Y;
        graphics.fill(jx, jy, jx + CraftingStationMenu.JOB_WIDTH - 18, jy + CraftingStationMenu.JOB_HEIGHT, PanelPainter.FACE);
        ItemStack result = menu.station().clientJobResult();
        PanelPainter.bordered(graphics, jx + 1, jy + 1, CELL - 2, CELL - 2, PanelPainter.DARK, result.isEmpty() ? PanelPainter.SLOT : GREEN);
        if (!result.isEmpty()) {
            graphics.item(result, jx + 1, jy + 1);
            graphics.itemDecorations(font, result, jx + 1, jy + 1);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        ItemStack result = menu.station().clientJobResult();
        int x = CraftingStationMenu.JOB_X;
        int y = CraftingStationMenu.JOB_Y + CELL + 4;
        Component text = result.isEmpty()
                ? Component.translatable("gui.servus_shuttles.crafting_station.idle")
                : Component.translatable("gui.servus_shuttles.crafting_station.left", menu.station().clientJobLeft());
        graphics.text(font, text, x, y, TEXT, false);
        if (!result.isEmpty() && menu.waitingForPower()) {
            graphics.text(font, Component.translatable("gui.servus_shuttles.crafting_station.no_power"), x, y + 10, 0xFFB71C1C, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return panel.mouseClicked(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }
}
