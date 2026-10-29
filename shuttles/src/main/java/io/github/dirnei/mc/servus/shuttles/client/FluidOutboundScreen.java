package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.foundation.client.MenuButtons;
import io.github.dirnei.mc.servus.shuttles.fluidstation.FluidKeep;
import io.github.dirnei.mc.servus.shuttles.fluidstation.SetFluidKeepPayload;
import io.github.dirnei.mc.servus.shuttles.menu.FluidOutboundMenu;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The Fluid Outbound GUI: the shared station GUI with the keep cell above
 * the gauge, a mark on the gauge at the keep amount, the amount below it and
 * the -1 B, +1 B, -10 B and +10 B buttons.
 */
public class FluidOutboundScreen extends FluidStationScreen<FluidOutboundMenu> {

    private static final int COLOUR_KEEP_MARK = 0xFFFFFFFF;
    private static final String[] STEP_LABELS = {"-1", "+1", "-10", "+10"};
    private static final int CELL_FLUID = 16;

    private final FluidGauge cellFluid = new FluidGauge(CELL_FLUID, CELL_FLUID);
    private final Button[] stepButtons = new Button[FluidOutboundMenu.KEEP_STEPS.length];

    public FluidOutboundScreen(FluidOutboundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, FluidOutboundMenu.IMAGE_HEIGHT, FluidOutboundMenu.GAUGE_Y, FluidOutboundMenu.STATE_Y);
    }

    @Override
    protected void init() {
        super.init();
        int width = (menu.pitch() - 6) / 2;
        for (int step = 0; step < stepButtons.length; step++) {
            int id = FluidOutboundMenu.keepButton(step);
            int x = leftPos + menu.columnX() + 2 + (step % 2) * (width + 2);
            int y = topPos + FluidOutboundMenu.BUTTONS_Y + (step / 2) * (FluidOutboundMenu.BUTTON_HEIGHT + 1);
            stepButtons[step] = addRenderableWidget(Button.builder(Component.literal(STEP_LABELS[step]),
                    button -> MenuButtons.press(minecraft, menu, id)).bounds(x, y, width, FluidOutboundMenu.BUTTON_HEIGHT).build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (Button button : stepButtons) {
            button.active = menu.keep().isSet();
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (overCell(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, font.split(cellTooltip(menu.keep()), 180), mouseX, mouseY);
        }
    }

    private Component cellTooltip(FluidKeep<Fluid> keep) {
        return keep.fluid() == null ? Component.translatable("gui.servus_shuttles.fluid_outbound.keep_cell.empty")
                : Component.translatable("gui.servus_shuttles.fluid_outbound.keep_cell", new FluidStack(keep.fluid(), 1).getHoverName());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos + menu.cellX();
        int y = topPos + FluidOutboundMenu.CELL_Y;
        PanelPainter.sunken(graphics, x, y, FluidOutboundMenu.CELL_SIZE, FluidOutboundMenu.CELL_SIZE);
        FluidKeep<Fluid> keep = menu.keep();
        if (keep.fluid() != null) {
            cellFluid.place(x + 1, y + 1);
            cellFluid.render(graphics, new FluidStack(keep.fluid(), CELL_FLUID), CELL_FLUID);
        }
        renderKeepMark(graphics, keep);
    }

    /** The line across the gauge at the keep amount. */
    private void renderKeepMark(GuiGraphicsExtractor graphics, FluidKeep<Fluid> keep) {
        int capacity = menu.capacity();
        if (keep.target() <= 0 || capacity <= 0) {
            return;
        }
        int height = FluidOutboundMenu.GAUGE_HEIGHT;
        int bottom = topPos + FluidOutboundMenu.GAUGE_Y + height;
        int y = Math.min(bottom - 1, bottom - (int) Math.min(height, (long) keep.target() * height / capacity));
        int x = leftPos + menu.gaugeX();
        graphics.fill(x, y, x + FluidOutboundMenu.GAUGE_WIDTH, y + 1, COLOUR_KEEP_MARK);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        Component amount = Component.translatable("gui.servus_shuttles.fluid_outbound.keep_amount", buckets(menu.keep().target()));
        int x = menu.columnX() + (menu.pitch() - font.width(amount)) / 2;
        graphics.text(font, amount, x, FluidOutboundMenu.AMOUNT_Y, COLOUR_TEXT, false);
    }

    /** An amount in buckets: whole buckets plain, else with one decimal. */
    private static String buckets(int millibuckets) {
        return millibuckets % 1000 == 0 ? Integer.toString(millibuckets / 1000) : String.format(Locale.ROOT, "%.1f", millibuckets / 1000.0);
    }

    private boolean overCell(double mouseX, double mouseY) {
        double x = mouseX - leftPos - menu.cellX();
        double y = mouseY - topPos - FluidOutboundMenu.CELL_Y;
        return x >= 0 && x < FluidOutboundMenu.CELL_SIZE && y >= 0 && y < FluidOutboundMenu.CELL_SIZE;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (overCell(event.x(), event.y())) {
            ClientPacketDistributor.sendToServer(SetFluidKeepPayload.INSTANCE);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
