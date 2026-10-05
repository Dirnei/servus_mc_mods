package io.github.dirnei.mc.servus.manufacturing.client;

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
        public static final Layout GENERATOR = new Layout(true, false, true, gui("burner_machine"));
        public static final Layout ASSEMBLER = new Layout(false, true, true, gui("assembler"));
        public static final Layout LAB = new Layout(false, true, true, gui("research_lab"));

        private static ResourceLocation gui(String name) {
            return ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "textures/gui/" + name + ".png");
        }
    }

    protected static final int ENERGY_X = 152;
    protected static final int ENERGY_Y = 17;
    protected static final int ENERGY_WIDTH = 12;
    protected static final int ENERGY_HEIGHT = 52;

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
        if (layout.energy() && isHovering(ENERGY_X, ENERGY_Y, ENERGY_WIDTH, ENERGY_HEIGHT, mouseX, mouseY)) {
            guiGraphics.renderTooltip(font,
                    Component.literal(menu.energyStored() + " / " + menu.energyCapacity() + " FE"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(layout.texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (layout.flame() && menu.isBurning()) {
            int flame = menu.burnScaled(13);
            guiGraphics.blit(layout.texture(), leftPos + 56, topPos + 36 + 12 - flame, 176, 12 - flame, 14, flame + 1);
        }
        if (layout.arrow()) {
            int arrow = menu.progressScaled(24);
            guiGraphics.blit(layout.texture(), leftPos + 79, topPos + 34, 176, 14, arrow + 1, 16);
        }
        if (layout.energy()) {
            int x = leftPos + ENERGY_X;
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
