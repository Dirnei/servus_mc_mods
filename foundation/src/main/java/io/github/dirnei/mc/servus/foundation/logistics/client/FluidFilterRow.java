package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.FluidSprites;
import io.github.dirnei.mc.servus.foundation.logistics.duct.FluidFilter;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The nine fluid filter slots of the Fluid Pipe page: each shows its fluid's
 * texture in its colour and names it in its tooltip. Reads the client copy of
 * the selected side's filter every frame. Positions are absolute.
 */
final class FluidFilterRow {

    private static final int STRIDE = 18;
    private static final int HOVERED = 0x80FFFFFF;

    private final Supplier<FluidFilter> filter;
    private int left;
    private int top;

    FluidFilterRow(Supplier<FluidFilter> filter) {
        this.filter = filter;
    }

    /** Where the 16 by 16 inside of the first slot sits. */
    void place(int x, int y) {
        left = x;
        top = y;
    }

    /** The slot under the mouse, or {@link FluidFilter#NONE}. */
    int slotAt(double mouseX, double mouseY) {
        if (mouseY < top || mouseY >= top + 16 || mouseX < left) {
            return FluidFilter.NONE;
        }
        int slot = (int) ((mouseX - left) / STRIDE);
        boolean inside = mouseX - left - slot * STRIDE < 16;
        return slot < FluidFilter.SLOTS && inside ? slot : FluidFilter.NONE;
    }

    /** The fluid in a slot as a stack of 1 mB, empty for an empty slot. */
    FluidStack fluid(int slot) {
        String id = filter.get().get(slot);
        Identifier parsed = id == null ? null : Identifier.tryParse(id);
        Fluid fluid = parsed == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(parsed);
        return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, 1);
    }

    void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int hovered = slotAt(mouseX, mouseY);
        for (int slot = 0; slot < FluidFilter.SLOTS; slot++) {
            int x = left + slot * STRIDE;
            FluidStack fluid = fluid(slot);
            if (!fluid.isEmpty()) {
                FluidSprites.drawIcon(graphics, fluid, x, top);
            }
            if (slot == hovered) {
                graphics.fill(x, top, x + 16, top + 16, HOVERED);
            }
        }
    }

    /** The hovered slot's fluid name; returns whether a tooltip was drawn. */
    boolean renderTooltip(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        int slot = slotAt(mouseX, mouseY);
        FluidStack fluid = slot == FluidFilter.NONE ? FluidStack.EMPTY : fluid(slot);
        if (fluid.isEmpty()) {
            return false;
        }
        graphics.setTooltipForNextFrame(font, fluid.getHoverName(), mouseX, mouseY);
        return true;
    }
}
