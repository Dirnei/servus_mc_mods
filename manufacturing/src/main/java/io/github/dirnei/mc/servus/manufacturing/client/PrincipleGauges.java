package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.fluids.FluidStack;

/** Three slim gauges for Salt, Sulfur and Mercury on the left of an alchemy machine's GUI. */
final class PrincipleGauges {

    private static final int X = 10;
    private static final int Y = 17;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 52;
    private static final int STEP = 14;

    private final FluidGauge[] gauges = new FluidGauge[Principle.values().length];
    private final PrincipleMenu menu;

    PrincipleGauges(PrincipleMenu menu) {
        this.menu = menu;
        for (int i = 0; i < gauges.length; i++) {
            gauges[i] = new FluidGauge(WIDTH, HEIGHT);
        }
    }

    private int left;
    private int top;

    void place(int leftPos, int topPos) {
        left = leftPos;
        top = topPos;
        for (int i = 0; i < gauges.length; i++) {
            gauges[i].place(leftPos + X + i * STEP, topPos + Y);
        }
    }

    /** The principle whose gauge is under the mouse, or null. */
    Principle principleAt(double mouseX, double mouseY) {
        for (Principle principle : Principle.values()) {
            int x = left + X + principle.ordinal() * STEP;
            int y = top + Y;
            if (mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT) {
                return principle;
            }
        }
        return null;
    }

    /** Sends a gauge click when a container is on the cursor; returns whether the click was a gauge click. */
    boolean click(net.minecraft.client.Minecraft minecraft, int containerId, net.minecraft.world.item.ItemStack carried,
                  double mouseX, double mouseY) {
        Principle principle = principleAt(mouseX, mouseY);
        if (principle == null || carried.isEmpty() || minecraft.gameMode == null) {
            return false;
        }
        minecraft.gameMode.handleInventoryButtonClick(containerId,
                io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleBuckets.GAUGE_BUTTON_FIRST + principle.ordinal());
        return true;
    }

    private FluidStack stack(Principle principle) {
        int amount = menu.principleAmount(principle);
        return amount <= 0 ? FluidStack.EMPTY : new FluidStack(PrincipleFluids.fluid(principle), amount);
    }

    void render(GuiGraphicsExtractor graphics) {
        for (Principle principle : Principle.values()) {
            gauges[principle.ordinal()].render(graphics, stack(principle), menu.principleCapacity());
        }
    }

    void renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        for (Principle principle : Principle.values()) {
            gauges[principle.ordinal()].renderTooltip(graphics, font, stack(principle), menu.principleCapacity(), mouseX, mouseY);
        }
    }
}
