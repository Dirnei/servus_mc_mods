package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.logistics.channel.Channel;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The channel of a line's side as one wide button: colour swatch, icon and name; a click opens the channel popup. */
public final class ChannelButton extends AbstractButton {

    public static final int HEIGHT = 18;
    private static final int TEXT = 0xFF202020;
    private static final int DARK = 0xFF373737;

    private final IntSupplier channel;
    private final Runnable onPress;

    public ChannelButton(int x, int y, int width, IntSupplier channel, Runnable onPress) {
        super(x, y, width, HEIGHT, Component.empty());
        this.channel = channel;
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        onPress.run();
    }

    @Override
    public Component getMessage() {
        return Component.translatable("gui.servus_foundation.channel.button", ChannelDisplay.name(ChannelDisplay.channel(channel.getAsInt())));
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX();
        int y0 = getY();
        int right = x0 + width;
        PopupPanel.bordered(graphics, x0 + 1, y0 + 1, width - 2, HEIGHT - 2, DARK, isHovered() ? 0xFFDDDDDD : 0xFFA0A0A0);
        graphics.fill(x0 + 1, y0 + 1, right - 1, y0 + 2, 0xFFFFFFFF);
        Channel shown = ChannelDisplay.channel(channel.getAsInt());
        ChannelDisplay.swatch(graphics, x0 + 4, y0 + 4, 10, shown.argb());
        int textX = x0 + 18;
        ItemStack icon = ChannelDisplay.icon(shown);
        if (!icon.isEmpty()) {
            graphics.item(icon, textX, y0 + 1);
            textX += 18;
        }
        String name = font.plainSubstrByWidth(ChannelDisplay.name(shown).getString(), right - 12 - textX);
        graphics.text(font, name, textX, y0 + 5, TEXT, false);
        graphics.text(font, "v", right - 8, y0 + 5, TEXT, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }
}
