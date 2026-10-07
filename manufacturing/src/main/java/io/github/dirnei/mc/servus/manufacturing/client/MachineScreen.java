package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.lib.side.RelativeSide;
import io.github.dirnei.mc.servus.lib.side.SideMode;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen shared by all machines. The layout says which indicators to draw and on which background. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    /** Which indicators a machine screen shows and its background texture. */
    public record Layout(boolean flame, boolean arrow, boolean energy, ResourceLocation texture) {
        public static final Layout BURNER = new Layout(true, true, false, gui("burner_machine"));
        public static final Layout GENERATOR = new Layout(true, false, true, gui("coal_generator"));
        public static final Layout ASSEMBLER = new Layout(false, false, true, gui("assembler"));
        public static final Layout LAB = new Layout(false, true, true, gui("research_lab"));
        public static final Layout ELECTRIC = new Layout(false, true, true, gui("electric_extractor"));
        public static final Layout CRUSHER = new Layout(false, true, true, gui("crusher"));
        public static final Layout LOADER = new Layout(false, false, true, gui("chunk_loader"));

        private static ResourceLocation gui(String name) {
            return ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "textures/gui/" + name + ".png");
        }
    }

    /** The furnace's own flame and arrow, so machines burn like vanilla does. */
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");

    /** Left edge of the energy bar; wider screens override. */
    protected int energyX() {
        return 152;
    }
    protected static final int ENERGY_Y = 17;
    protected static final int ENERGY_WIDTH = 12;
    protected static final int ENERGY_HEIGHT = 52;

    /** Side panel left of the GUI: 3 by 3 cells of 14 pixels. */
    private static final int SIDE_CELL = 14;
    private static final int SIDE_PANEL_WIDTH = SIDE_CELL * 3 + 4;
    private static final int SIDE_PANEL_GAP = 4;
    private static final int SIDE_PANEL_Y = 17;
    /** Grid position (column, row) of each relative side. */
    private static final int[][] SIDE_CELLS = {
            {1, 1}, // FRONT
            {1, 2}, // BACK
            {0, 1}, // LEFT
            {2, 1}, // RIGHT
            {1, 0}, // TOP
            {2, 2}, // BOTTOM
    };

    protected final Layout layout;

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title, Layout layout) {
        super(menu, playerInventory, title);
        this.layout = layout;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    public static MachineScreen burner(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.BURNER);
    }

    public static MachineScreen electric(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.ELECTRIC);
    }

    public static MachineScreen crusher(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.CRUSHER);
    }

    public static MachineScreen generator(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.GENERATOR);
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
        if (layout.energy() && isHovering(energyX(), ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"), mouseX, mouseY);
        }
        RelativeSide hoveredSide = sideAt(mouseX, mouseY);
        if (hoveredSide != null) {
            SideMode mode = menu.sideMode(hoveredSide);
            guiGraphics.renderTooltip(font, Component.translatable("gui.servus_manufacturing.side." + hoveredSide.name().toLowerCase())
                    .append(": ")
                    .append(Component.translatable("gui.servus_manufacturing.side_mode." + mode.name().toLowerCase())), mouseX, mouseY);
        }
    }

    // Side panel

    private int sidePanelX() {
        return leftPos - SIDE_PANEL_GAP - SIDE_PANEL_WIDTH;
    }

    private int sidePanelY() {
        return topPos + SIDE_PANEL_Y;
    }

    private int sideCellX(RelativeSide side) {
        return sidePanelX() + 2 + SIDE_CELLS[side.ordinal()][0] * SIDE_CELL;
    }

    private int sideCellY(RelativeSide side) {
        return sidePanelY() + 2 + SIDE_CELLS[side.ordinal()][1] * SIDE_CELL;
    }

    private RelativeSide sideAt(double mouseX, double mouseY) {
        for (RelativeSide side : RelativeSide.values()) {
            int x = sideCellX(side);
            int y = sideCellY(side);
            if (mouseX >= x && mouseX < x + SIDE_CELL && mouseY >= y && mouseY < y + SIDE_CELL) {
                return side;
            }
        }
        return null;
    }

    private static int sideColor(SideMode mode) {
        return switch (mode) {
            case AUTO -> 0xFF8B8B8B;
            case INPUT -> 0xFF4C7FD6;
            case OUTPUT -> 0xFFE08A2E;
            case BOTH -> 0xFF5CB85C;
            case DISABLED -> 0xFF7A2E2E;
        };
    }

    private void renderSidePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = sidePanelX();
        int y = sidePanelY();
        int size = SIDE_CELL * 3 + 4;
        guiGraphics.fill(x, y, x + size, y + size, 0xFF373737);
        guiGraphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, 0xFFC6C6C6);
        RelativeSide hovered = sideAt(mouseX, mouseY);
        for (RelativeSide side : RelativeSide.values()) {
            int cx = sideCellX(side);
            int cy = sideCellY(side);
            guiGraphics.fill(cx, cy, cx + SIDE_CELL, cy + SIDE_CELL, side == hovered ? 0xFFFFFFFF : 0xFF555555);
            guiGraphics.fill(cx + 1, cy + 1, cx + SIDE_CELL - 1, cy + SIDE_CELL - 1, sideColor(menu.sideMode(side)));
            String letter = side.name().substring(0, 1);
            guiGraphics.drawString(font, letter, cx + 4, cy + 3, 0xFF202020, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RelativeSide side = sideAt(mouseX, mouseY);
        if (side != null && minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MachineMenu.SIDE_BUTTON_FIRST + side.ordinal());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        renderSidePanel(guiGraphics, mouseX, mouseY);
        if (layout.flame() && menu.isBurning()) {
            int flame = menu.burnScaled(13) + 1;
            guiGraphics.blitSprite(LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - flame, leftPos + 56, topPos + 36 + 14 - flame, 14, flame);
        }
        if (layout.arrow()) {
            int arrow = menu.progressScaled(24);
            if (arrow > 0) {
                guiGraphics.blitSprite(BURN_PROGRESS_SPRITE, 24, 16, 0, 0, leftPos + 79, topPos + 34, arrow, 16);
            }
        }
        if (layout.energy()) {
            int x = leftPos + energyX();
            int y = topPos + ENERGY_Y;
            guiGraphics.fill(x - 1, y - 1, x + ENERGY_WIDTH + 1, y + ENERGY_HEIGHT + 1, 0xFF373737);
            guiGraphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFF1E1E1E);
            int filled = menu.energyScaled(ENERGY_HEIGHT);
            if (filled > 0) {
                guiGraphics.fill(x, y + ENERGY_HEIGHT - filled, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFFD03030);
            }
        }
    }
}
