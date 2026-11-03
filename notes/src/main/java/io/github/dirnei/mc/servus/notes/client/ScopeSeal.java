package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.model.Scope;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** The scope of a list as a small wax seal: a rounded coloured badge with the scope's name. */
final class ScopeSeal {

    static final int HEIGHT = 10;
    private static final int PADDING = 4;
    private static final int INK = 0xFFFFFFFF;
    private static final int SHADE = 0x40000000;
    private static final int SHINE = 0x30FFFFFF;

    private ScopeSeal() {
    }

    static Component word(Scope scope) {
        return Component.translatable("gui.servus_notes.notebook.scope." + scope.key());
    }

    static int width(Font font, Scope scope) {
        return font.width(word(scope)) + 2 * PADDING;
    }

    /** Draws the seal with its top left corner at {@code x, y}; returns its width. */
    static int draw(GuiGraphicsExtractor graphics, Font font, Scope scope, int x, int y) {
        int width = width(font, scope);
        int colour = colour(scope);
        graphics.fill(x + 1, y, x + width - 1, y + HEIGHT, colour);
        graphics.fill(x, y + 1, x + width, y + HEIGHT - 1, colour);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, SHINE);
        graphics.fill(x + 1, y + HEIGHT - 2, x + width - 1, y + HEIGHT - 1, SHADE);
        graphics.text(font, word(scope), x + PADDING, y + 1, INK, false);
        return width;
    }

    private static int colour(Scope scope) {
        return switch (scope) {
            case PRIVATE -> 0xFF6A5A4A;
            case TEAM -> 0xFF2F5AA0;
            case SERVER -> 0xFFA8322A;
        };
    }
}
