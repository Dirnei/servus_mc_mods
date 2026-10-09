package io.github.dirnei.mc.servus.manufacturing.client;

import io.github.dirnei.mc.servus.foundation.client.FluidGauge;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleFluids;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    void place(int leftPos, int topPos) {
        for (int i = 0; i < gauges.length; i++) {
            gauges[i].place(leftPos + X + i * STEP, topPos + Y);
        }
    }

    private FluidStack stack(Principle principle) {
        int amount = menu.principleAmount(principle);
        return amount <= 0 ? FluidStack.EMPTY : new FluidStack(PrincipleFluids.fluid(principle), amount);
    }

    void render(GuiGraphics graphics) {
        for (Principle principle : Principle.values()) {
            gauges[principle.ordinal()].render(graphics, stack(principle), menu.principleCapacity());
        }
    }

    void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        for (Principle principle : Principle.values()) {
            gauges[principle.ordinal()].renderTooltip(graphics, font, stack(principle), menu.principleCapacity(), mouseX, mouseY);
        }
    }
}
