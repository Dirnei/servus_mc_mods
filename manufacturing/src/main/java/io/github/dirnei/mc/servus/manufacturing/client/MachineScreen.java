package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.lib.block.SideConfigMenus;
import io.github.dirnei.mc.servus.lib.client.SideConfigPanel;
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

    /** The shared side panel left of the GUI. */
    private final SideConfigPanel panel = new SideConfigPanel(menu::sideMode, side -> {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, SideConfigMenus.buttonOf(side));
        }
    });

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
        panel.placeBeside(leftPos, topPos);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (layout.energy() && isHovering(energyX(), ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"), mouseX, mouseY);
        }
        panel.renderTooltip(guiGraphics, font, mouseX, mouseY);
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
        guiGraphics.blit(layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        panel.render(guiGraphics, font, mouseX, mouseY);
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
