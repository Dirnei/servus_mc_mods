package io.github.dirnei.mc.servus.logistics.client;

import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.logistics.ServusLogisticsIds;
import io.github.dirnei.mc.servus.logistics.duct.Line;
import io.github.dirnei.mc.servus.logistics.menu.UtilityDuctMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Side selector, mode buttons, filter row, priority and remove buttons for one duct. */
public class UtilityDuctScreen extends AbstractContainerScreen<UtilityDuctMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusLogisticsIds.MOD_ID, "textures/gui/utility_duct.png");
    private static final int SIDE_Y = 18;
    private static final int MODE_Y = 38;
    private static final int PRIORITY_Y = 86;
    private static final int REMOVE_Y = 106;
    private static final int CELL = 18;
    private static final String[] SIDE_SHORT = {"D", "U", "N", "S", "W", "E"};

    private final List<Button> modeButtons = new ArrayList<>();
    private List<Line> lines = List.of();

    public UtilityDuctScreen(UtilityDuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 212;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = UtilityDuctMenu.INVENTORY_Y - 11;
        rebuildWidgets();
    }

    private void rebuildButtons() {
        clearWidgets();
        modeButtons.clear();
        lines = new ArrayList<>(menu.duct().lines());
        for (int i = 0; i < Direction.values().length; i++) {
            int id = UtilityDuctMenu.BUTTON_SELECT_SIDE + i;
            addRenderableWidget(Button.builder(Component.literal(SIDE_SHORT[i]), button -> press(id))
                    .bounds(leftPos + 8 + i * CELL, topPos + SIDE_Y, 16, 16).build());
        }
        for (int c = 0; c < lines.size(); c++) {
            Line line = lines.get(c);
            int x = leftPos + 8 + c * 82;
            Button mode = Button.builder(Component.empty(), button -> press(UtilityDuctMenu.BUTTON_CYCLE_MODE + line.ordinal()))
                    .bounds(x, topPos + MODE_Y, 78, 18).build();
            modeButtons.add(mode);
            addRenderableWidget(mode);
            addRenderableWidget(Button.builder(Component.translatable("gui.servus_logistics.remove." + line.id()),
                            button -> press(UtilityDuctMenu.BUTTON_REMOVE_LINE + line.ordinal()))
                    .bounds(x, topPos + REMOVE_Y, 78, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("-"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_DOWN))
                .bounds(leftPos + 8, topPos + PRIORITY_Y, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> press(UtilityDuctMenu.BUTTON_PRIORITY_UP))
                .bounds(leftPos + 60, topPos + PRIORITY_Y, 16, 16).build());
    }

    @Override
    protected void rebuildWidgets() {
        super.rebuildWidgets();
        rebuildButtons();
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            menu.clickMenuButton(minecraft.player, id);
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (lines.size() != menu.duct().lines().size()) {
            rebuildButtons();
        }
        Direction side = menu.side();
        for (int c = 0; c < lines.size() && c < modeButtons.size(); c++) {
            SideMode mode = menu.duct().mode(lines.get(c), side);
            modeButtons.get(c).setMessage(Component.translatable("gui.servus_logistics.line." + lines.get(c).id())
                    .append(": ").append(Component.translatable("gui.servus_logistics.mode." + mode.name().toLowerCase(Locale.ROOT))));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int selected = menu.side().ordinal();
        guiGraphics.fill(leftPos + 7 + selected * CELL, topPos + SIDE_Y - 1, leftPos + 7 + selected * CELL + 18, topPos + SIDE_Y + 17, 0xFF40C040);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        Direction side = menu.side();
        guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.side." + side.getSerializedName()),
                120, SIDE_Y + 4, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.filter"), 8, UtilityDuctMenu.FILTER_Y - 10, 0x404040, false);
        guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.priority", menu.priority()), 28, PRIORITY_Y + 4, 0x404040, false);
        if (lines.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("gui.servus_logistics.no_lines"), 8, MODE_Y + 5, 0x404040, false);
        }
    }
}
