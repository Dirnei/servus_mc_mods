package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineStatus;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.side.SideLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Screen shared by all machines, named or not. The layout says which indicators to draw and on which background. */
public class MachineScreen extends NamedMachineScreen<MachineMenu> {

    /**
     * Which indicators a machine screen shows, whether it has the side panel, its background texture, and whether a
     * status line takes the place of the inventory label.
     */
    public record Layout(boolean flame, boolean arrow, boolean energy, boolean fluid, boolean sidePanel, boolean statusLine,
                         Identifier texture) {

        /** A layout whose status line shows exactly when it has a fluid gauge. */
        public Layout(boolean flame, boolean arrow, boolean energy, boolean fluid, boolean sidePanel, Identifier texture) {
            this(flame, arrow, energy, fluid, sidePanel, fluid, texture);
        }

        /** A layout without fluid gauge or status line, with the side panel. */
        public Layout(boolean flame, boolean arrow, boolean energy, Identifier texture) {
            this(flame, arrow, energy, false, true, texture);
        }

        /** A GUI background at {@code <namespace>:textures/gui/<name>.png}. */
        public static Identifier gui(String namespace, String name) {
            return Identifier.fromNamespaceAndPath(namespace, "textures/gui/" + name + ".png");
        }
    }

    /** The furnace's own flame and arrow, so machines burn like vanilla does. */
    private static final Identifier LIT_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/furnace/burn_progress");

    protected static final int ENERGY_Y = 17;
    protected static final int ENERGY_WIDTH = 12;
    protected static final int ENERGY_HEIGHT = 52;

    /** Where the fluid gauge sits, over the place of an output slot. */
    protected static final int FLUID_X = 116;
    protected static final int FLUID_Y = 17;
    protected static final int FLUID_WIDTH = 16;
    protected static final int FLUID_HEIGHT = 52;
    private static final int STATUS_OK = 0xFF3F7F3F;
    private static final int STATUS_MISSING = 0xFFA03030;
    private static final int STATUS_WAITING = 0xFF404040;
    private static final int RESTART_MARK = 0xFFB0B0B0;
    private static final int RESTART_MARK_LENGTH = 3;

    private final FluidGauge fluidGauge = new FluidGauge(FLUID_WIDTH, FLUID_HEIGHT);

    /** The shared side panel left of the GUI; sides the machine fixes are drawn greyed. */
    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> menu.machine().sideConfig().isLocked(side),
            side -> clickButton(SideConfigMenus.buttonOf(side)));

    protected final Layout layout;

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title, Layout layout) {
        this(menu, playerInventory, title, layout, 176, 166);
    }

    /** A machine screen with its own panel size. */
    protected MachineScreen(MachineMenu menu, Inventory playerInventory, Component title, Layout layout, int width, int height) {
        super(menu, playerInventory, title, width, height);
        this.layout = layout;
    }

    /** Left edge of the energy bar; wider screens override. */
    protected int energyX() {
        return 152;
    }

    /** Top edge of the energy bar; screens with their own bar override. */
    protected int energyY() {
        return ENERGY_Y;
    }

    protected int energyWidth() {
        return ENERGY_WIDTH;
    }

    protected int energyHeight() {
        return ENERGY_HEIGHT;
    }

    /** Machines without side settings, like pumps, show no side panel. */
    protected boolean showsSidePanel() {
        return layout.sidePanel();
    }

    /** Sends a menu button click to the server; returns whether it was sent. */
    protected boolean clickButton(int buttonId) {
        return MenuButtons.press(minecraft, menu, buttonId);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        if (showsSidePanel()) {
            panel.placeBeside(leftPos, topPos);
            if (menu.machine().fluidSides() != null) {
                panel.enableFluids(menu::fluidSide, menu.machine()::sideTankLabels, side -> menu.machine().fluidSides().isLocked(side),
                        side -> clickButton(SideConfigMenus.buttonOf(SideLayer.FLUIDS, side)),
                        side -> clickButton(SideConfigMenus.buttonOfTank(side)));
            }
        }
        fluidGauge.place(leftPos + FLUID_X, topPos + FLUID_Y);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (layout.energy() && isHovering(energyX(), energyY(), energyWidth(), energyHeight(), mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(font,
                    Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"), mouseX, mouseY);
        }
        if (layout.fluid()) {
            fluidGauge.renderTooltip(guiGraphics, font, menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
        }
        if (showsSidePanel()) {
            panel.renderTooltip(guiGraphics, font, mouseX, mouseY);
        }
    }

    /** Machines with a status line show it where other machines show the inventory label. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!layout.statusLine()) {
            super.extractLabels(guiGraphics, mouseX, mouseY);
            return;
        }
        extractTitle(guiGraphics);
        StatusLine line = statusLine();
        if (line != null) {
            textFitted(guiGraphics, line.text(), inventoryLabelX, inventoryLabelY, imageWidth - 2 * inventoryLabelX, line.color());
        }
    }

    private record StatusLine(Component text, int color) {
    }

    /**
     * Why the machine is dark, or that it runs; null when there is nothing to say. A pump names a missing water source
     * first, since without one it cannot run at all.
     */
    private StatusLine statusLine() {
        if (layout.fluid() && !menu.sourceValid()) {
            return new StatusLine(Component.translatable("gui.servus_foundation.pump.source_missing"), STATUS_MISSING);
        }
        MachineStatus status = menu.machineStatus();
        return switch (status) {
            case WAITING -> new StatusLine(Component.translatable(layout.fluid()
                    ? "gui.servus_foundation.machine.waiting.fluid" : "gui.servus_foundation.machine.waiting.energy",
                    menu.restartLevel()), STATUS_WAITING);
            case OUT_OF_FUEL -> new StatusLine(Component.translatable("gui.servus_foundation.machine.out_of_fuel"), STATUS_MISSING);
            case NO_POWER -> new StatusLine(Component.translatable("gui.servus_foundation.machine.no_power"), STATUS_MISSING);
            case RUNNING, IDLE -> runningLine(status);
        };
    }

    /** A pump that can run names its source; any other machine says it runs, and nothing while idle. */
    private StatusLine runningLine(MachineStatus status) {
        if (layout.fluid()) {
            return new StatusLine(Component.translatable("gui.servus_foundation.pump.source_ok"), STATUS_OK);
        }
        return status == MachineStatus.RUNNING
                ? new StatusLine(Component.translatable("gui.servus_foundation.machine.running"), STATUS_OK) : null;
    }

    /**
     * A short notch on both edges of a bar at the restart level, while the level lies inside the bar. The coordinates are
     * the bar's inside on screen.
     */
    private void extractRestartMark(GuiGraphicsExtractor guiGraphics, int x, int y, int width, int height, int capacity) {
        int level = menu.restartLevel();
        if (!layout.statusLine() || capacity <= 0 || level <= 0 || level >= capacity) {
            return;
        }
        int markY = y + height - (int) ((long) level * height / capacity);
        guiGraphics.fill(x, markY, x + RESTART_MARK_LENGTH, markY + 1, RESTART_MARK);
        guiGraphics.fill(x + width - RESTART_MARK_LENGTH, markY, x + width, markY + 1, RESTART_MARK);
    }

    /** Draws one line of text, scaled down about its middle when it is wider than {@code available}. */
    protected void textFitted(GuiGraphicsExtractor guiGraphics, Component line, int x, int y, int available, int color) {
        int width = font.width(line);
        float scale = width > available ? (float) available / width : 1F;
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(x, y + (1F - scale) * font.lineHeight / 2F);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.text(font, line, 0, 0, color, false);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (showsSidePanel() && panel.mouseClicked(event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (showsSidePanel()) {
            panel.render(guiGraphics, font, mouseX, mouseY);
        }
        if (layout.flame() && menu.isBurning()) {
            int flame = menu.burnScaled(13) + 1;
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE, 14, 14, 0, 14 - flame, leftPos + 56, topPos + 36 + 14 - flame, 14, flame);
        }
        if (layout.arrow()) {
            int arrow = menu.progressScaled(24);
            if (arrow > 0) {
                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE, 24, 16, 0, 0, leftPos + 79, topPos + 34, arrow, 16);
            }
        }
        if (layout.fluid()) {
            fluidGauge.render(guiGraphics, menu.fluid(), menu.fluidCapacity());
            extractRestartMark(guiGraphics, leftPos + FLUID_X, topPos + FLUID_Y, FLUID_WIDTH, FLUID_HEIGHT, menu.fluidCapacity());
        }
        if (layout.energy()) {
            extractEnergyBar(guiGraphics);
        }
    }

    /** The shared red energy bar; screens with their own look override. */
    protected void extractEnergyBar(GuiGraphicsExtractor guiGraphics) {
        int x = leftPos + energyX();
        int y = topPos + ENERGY_Y;
        guiGraphics.fill(x - 1, y - 1, x + ENERGY_WIDTH + 1, y + ENERGY_HEIGHT + 1, 0xFF373737);
        guiGraphics.fill(x, y, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFF1E1E1E);
        int filled = menu.energyScaled(ENERGY_HEIGHT);
        if (filled > 0) {
            guiGraphics.fill(x, y + ENERGY_HEIGHT - filled, x + ENERGY_WIDTH, y + ENERGY_HEIGHT, 0xFFD03030);
        }
        if (!layout.fluid()) {
            extractRestartMark(guiGraphics, x, y, ENERGY_WIDTH, ENERGY_HEIGHT, menu.energyCapacity());
        }
    }
}
