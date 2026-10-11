package io.github.dirnei.mc.servus.foundation.logistics.client;

import net.minecraft.client.input.InputWithModifiers;
import io.github.dirnei.mc.servus.foundation.logistics.duct.DuctChannels;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** A small square filled with a channel's color; the current channel gets a white frame. */
public final class ChannelButton extends AbstractButton {

    private static final int BORDER = 0xFF202020;
    private static final int FRAME = 0xFFFFFFFF;

    private final int channel;
    private final BooleanSupplier selected;
    private final Runnable onPress;

    public ChannelButton(int x, int y, int size, int channel, BooleanSupplier selected, Runnable onPress) {
        super(x, y, size, size, Component.translatable(DuctChannels.nameKey(channel)));
        this.channel = channel;
        this.selected = selected;
        this.onPress = onPress;
        setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + width;
        int y1 = y0 + height;
        guiGraphics.fill(x0, y0, x1, y1, selected.getAsBoolean() ? FRAME : BORDER);
        int color = DuctChannels.color(channel);
        if (isHovered()) {
            color = brighten(color);
        }
        guiGraphics.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, color);
        if (selected.getAsBoolean()) {
            guiGraphics.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, BORDER);
            guiGraphics.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, color);
        }
    }

    private static int brighten(int argb) {
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 40);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 40);
        int b = Math.min(255, (argb & 0xFF) + 40);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
