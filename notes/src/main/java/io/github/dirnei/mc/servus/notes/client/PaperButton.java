package io.github.dirnei.mc.servus.notes.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** The notebook's buttons and fields: slips of darker paper with a shadow edge below, drawn in book coordinates. */
final class PaperButton {

    static final int HEIGHT = 12;
    /** The height of a one line text field. */
    static final int FIELD_HEIGHT = 13;
    private static final int PADDING = 4;
    private static final int FACE = 0xFFE6D3A8;
    private static final int FACE_HOVER = 0xFFF3E2B8;
    private static final int EDGE = 0xFF8A6A3A;
    private static final int ON_FACE = 0xFF3F7F3A;
    private static final int ON_EDGE = 0xFF24501F;
    private static final int ON_INK = 0xFFFFFFFF;
    private static final int LIT_FACE = 0xFFFFE98A;
    private static final int FIELD = 0xFFFFF8E4;
    private static final int FIELD_FOCUSED = 0xFFFFFDF2;
    private static final int FIELD_EDGE = 0xFFC9B07A;

    private PaperButton() {
    }

    static int width(Font font, Component label) {
        return font.width(label) + 2 * PADDING;
    }

    /** Draws the button with its left edge at {@code x}, or across {@code width} when it is wider than the label. */
    static void draw(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, boolean hovered, int ink) {
        drawFace(graphics, font, label, x, y, width, hovered ? FACE_HOVER : FACE, EDGE, ink);
    }

    /** A button of a choice: green with white ink while it is the chosen one. */
    static void drawChoice(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, boolean hovered, boolean on) {
        if (on) {
            drawFace(graphics, font, label, x, y, width, ON_FACE, ON_EDGE, ON_INK);
        } else {
            draw(graphics, font, label, x, y, width, hovered, NotebookScreen.INK);
        }
    }

    /** A switch that is lit yellow while it is on. */
    static void drawLit(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, boolean hovered, boolean on) {
        drawFace(graphics, font, label, x, y, width, on ? LIT_FACE : hovered ? FACE_HOVER : FACE, EDGE, NotebookScreen.INK);
    }

    /** The light paper and edge of a text field. */
    static void drawField(GuiGraphicsExtractor graphics, int x, int y, int width, int height, boolean focused) {
        graphics.fill(x, y, x + width, y + height, focused ? FIELD_FOCUSED : FIELD);
        graphics.outline(x, y, width, height, focused ? EDGE : FIELD_EDGE);
    }

    private static void drawFace(GuiGraphicsExtractor graphics, Font font, Component label, int x, int y, int width, int face, int edge, int ink) {
        graphics.fill(x, y, x + width, y + HEIGHT - 1, face);
        graphics.outline(x, y, width, HEIGHT - 1, edge);
        graphics.fill(x + 1, y + HEIGHT - 1, x + width - 1, y + HEIGHT, edge);
        graphics.text(font, label, x + (width - font.width(label)) / 2, y + 2, ink, false);
    }
}
