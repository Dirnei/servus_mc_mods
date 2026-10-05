package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.menu.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Screen shared by all machines. The layout says which indicators to draw. */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    /** Which indicators a machine screen shows. */
    public record Layout(boolean flame, boolean arrow, boolean energy) {
        public static final Layout BURNER = new Layout(true, true, false);
        public static final Layout GENERATOR = new Layout(true, false, true);
        public static final Layout POWERED = new Layout(false, true, true);
    }

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "textures/gui/burner_machine.png");
    private static final int ENERGY_X = 152;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 52;

    private final Layout layout;

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

    public static MachineScreen powered(MachineMenu menu, Inventory playerInventory, Component title) {
        return new MachineScreen(menu, playerInventory, title, Layout.POWERED);
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
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (layout.flame() && menu.isBurning()) {
            int flame = menu.burnScaled(13);
            guiGraphics.blit(TEXTURE, leftPos + 56, topPos + 36 + 12 - flame, 176, 12 - flame, 14, flame + 1);
        }
        if (layout.arrow()) {
            int arrow = menu.progressScaled(24);
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, arrow + 1, 16);
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
