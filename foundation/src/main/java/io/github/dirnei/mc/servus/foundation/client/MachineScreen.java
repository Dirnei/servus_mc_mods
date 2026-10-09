package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.block.SideConfigMenus;
import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.SideConfigPanel;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen shared by all machines. The layout says which indicators to draw and on which background. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    /** Which indicators a machine screen shows, whether it has the side panel, and its background texture. */
    public record Layout(boolean flame, boolean arrow, boolean energy, boolean fluid, boolean sidePanel, ResourceLocation texture) {
        /** The shared burner background: fuel, input, arrow and output. */
        public static final Layout BURNER = new Layout(true, true, false, gui(ServusFoundationIds.MOD_ID, "burner_machine"));

        public Layout(boolean flame, boolean arrow, boolean energy, ResourceLocation texture) {
            this(flame, arrow, energy, false, true, texture);
        }

        /** A GUI background at {@code <namespace>:textures/gui/<name>.png}. */
        public static ResourceLocation gui(String namespace, String name) {
            return ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + name + ".png");
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

    /** Where the fluid gauge sits, over the place of an output slot. */
    protected static final int FLUID_X = 116;
    protected static final int FLUID_Y = 17;
    protected static final int FLUID_WIDTH = 16;
    protected static final int FLUID_HEIGHT = 52;
    private static final int STATUS_OK = 0xFF3F7F3F;
    private static final int STATUS_MISSING = 0xFFA03030;

    private final FluidGauge fluidGauge = new FluidGauge(FLUID_WIDTH, FLUID_HEIGHT);

    /** The shared side panel left of the GUI; sides the machine fixes are drawn greyed. */
    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> menu.machine().sideConfig().isLocked(side), side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

    protected final Layout layout;

    /** Machines without side settings, like pumps, show no side panel. */
    protected boolean showsSidePanel() {
        return layout.sidePanel();
    }

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title, Layout layout) {
        super(menu, playerInventory, title);
        this.layout = layout;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    public static MachineScreen burner(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.BURNER);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = this.imageHeight - 94;
        if (showsSidePanel()) {
            panel.placeBeside(leftPos, topPos);
        }
        fluidGauge.place(leftPos + FLUID_X, topPos + FLUID_Y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (layout.energy() && isHovering(energyX(), ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"), mouseX, mouseY);
        }
        if (layout.fluid()) {
            fluidGauge.renderTooltip(guiGraphics, font, menu.fluid(), menu.fluidCapacity(), mouseX, mouseY);
        }
        if (showsSidePanel()) {
            panel.renderTooltip(guiGraphics, font, mouseX, mouseY);
        }
    }

    /** Pumps show their source status where other machines show the inventory label. */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!layout.fluid()) {
            super.renderLabels(guiGraphics, mouseX, mouseY);
            return;
        }
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xFF404040, false);
        Component status = Component.translatable(menu.sourceValid()
                ? "gui.servus_foundation.pump.source_ok" : "gui.servus_foundation.pump.source_missing");
        int available = imageWidth - 2 * inventoryLabelX;
        int width = font.width(status);
        float scale = width > available ? (float) available / width : 1F;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(inventoryLabelX, inventoryLabelY + (1F - scale) * font.lineHeight / 2F, 0F);
        guiGraphics.pose().scale(scale, scale, 1F);
        guiGraphics.drawString(font, status, 0, 0, menu.sourceValid() ? STATUS_OK : STATUS_MISSING, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showsSidePanel() && panel.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (showsSidePanel()) {
            panel.render(guiGraphics, font, mouseX, mouseY);
        }
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
        if (layout.fluid()) {
            fluidGauge.render(guiGraphics, menu.fluid(), menu.fluidCapacity());
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
