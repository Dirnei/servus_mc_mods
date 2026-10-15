package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Plain buttons and small icons drawn inside the channel popup and form; absolute screen coordinates. */
final class PopupButtons {

    static final int DARK = 0xFF373737;
    static final int TEXT = 0xFF202020;
    static final int GREY = 0xFF8A8A8A;

    private PopupButtons() {
    }

    static boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** A framed button with a centred label; hovered buttons are lighter, disabled ones grey. */
    static void button(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height, Component label,
                       boolean hovered, boolean enabled) {
        int face = !enabled ? 0xFF8A8A8A : hovered ? 0xFFDDDDDD : 0xFFA0A0A0;
        PopupPanel.bordered(graphics, x + 1, y + 1, width - 2, height - 2, DARK, face);
        if (enabled) {
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFFFFFFFF);
        }
        String text = font.plainSubstrByWidth(label.getString(), width - 4);
        graphics.text(font, text, x + (width - font.width(text)) / 2, y + (height - 8) / 2, enabled ? TEXT : 0xFF505050, false);
    }

    /** A pencil in a 10 by 10 box at {@code x, y}. */
    static void pencil(GuiGraphicsExtractor graphics, int x, int y, int colour) {
        for (int i = 0; i < 7; i++) {
            graphics.fill(x + 2 + i, y + 7 - i, x + 4 + i, y + 9 - i, colour);
        }
        graphics.fill(x + 1, y + 8, x + 2, y + 10, colour);
    }

    /** A rubbish bin in a 10 by 10 box at {@code x, y}. */
    static void trash(GuiGraphicsExtractor graphics, int x, int y, int colour) {
        graphics.fill(x + 3, y, x + 7, y + 1, colour);
        graphics.fill(x + 1, y + 1, x + 9, y + 2, colour);
        graphics.fill(x + 2, y + 3, x + 8, y + 4, colour);
        graphics.fill(x + 2, y + 3, x + 3, y + 10, colour);
        graphics.fill(x + 7, y + 3, x + 8, y + 10, colour);
        graphics.fill(x + 2, y + 9, x + 8, y + 10, colour);
        graphics.fill(x + 4, y + 5, x + 5, y + 8, colour);
        graphics.fill(x + 5, y + 5, x + 6, y + 8, colour);
    }
}
