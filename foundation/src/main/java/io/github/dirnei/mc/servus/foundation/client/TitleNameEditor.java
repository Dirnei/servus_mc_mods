package io.github.dirnei.mc.servus.foundation.client;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A name shown as the title of a GUI, renamed in place: the name is plain text with a
 * pencil button after it; the pencil turns the title into a text box with a check
 * button. Enter, the check or a click anywhere else saves, Escape cancels. An empty
 * name shows the placeholder. The owning screen forwards {@link #layout},
 * {@link #mouseClicked}, {@link #keyPressed}, {@link #tick} and {@link #removed}.
 */
public final class TitleNameEditor {

    /** Size of the pencil and check button. */
    public static final int BUTTON = 11;
    private static final int GAP = 3;
    private static final int BOX_HEIGHT = 12;
    private static final int TITLE_COLOUR = 0xFF404040;

    private final int maxLength;
    private final UnaryOperator<String> clean;
    private final Consumer<String> send;
    private final Consumer<GuiEventListener> focus;
    private final Component placeholder;
    private final Component boxName;

    private String sentName;
    private boolean editing;
    private EditBox box;
    private EditButton button;
    private int rowX;
    private int rowY;
    private int rowWidth;

    /**
     * @param name        the current name, empty when unnamed
     * @param placeholder what the title shows while unnamed
     * @param boxName     the text box's narrated name
     * @param clean       how the receiver cleans a typed name, to skip sending an unchanged one
     * @param send        sends a typed name to the server
     * @param focus       moves the screen's focus, null to clear it
     */
    public TitleNameEditor(String name, Component placeholder, Component boxName, int maxLength, UnaryOperator<String> clean,
                           Consumer<String> send, Consumer<GuiEventListener> focus) {
        this.sentName = name;
        this.placeholder = placeholder;
        this.boxName = boxName;
        this.maxLength = maxLength;
        this.clean = clean;
        this.send = send;
        this.focus = focus;
    }

    /** Makes the text box and the button for a screen's {@code init}; the screen adds both as renderable widgets. */
    public EditBox createBox(Font font) {
        String value = editing && box != null ? box.getValue() : sentName;
        box = new EditBox(font, 0, 0, 10, BOX_HEIGHT, boxName);
        box.setMaxLength(maxLength);
        if (rowWidth > 0) {
            placeBox();
        }
        box.setValue(value);
        box.setVisible(editing);
        return box;
    }

    /** The pencil or check button; call after {@link #createBox}. */
    public AbstractButton createButton() {
        button = new EditButton(this::toggle);
        button.refresh(editing);
        return button;
    }

    /**
     * Places the title row: {@code x}, {@code y} where the title text starts, absolute, and the
     * width the name, the box and the button may take. Call every frame before the widgets draw.
     */
    public void layout(Font font, int x, int y, int width) {
        rowX = x;
        rowY = y;
        rowWidth = width;
        if (box == null || button == null) {
            return;
        }
        int textRoom = width - BUTTON - GAP;
        if (editing) {
            placeBox();
            button.setPosition(x + width - BUTTON, y - 2);
        } else {
            button.setPosition(x + Math.min(font.width(shownTitle()), textRoom) + GAP, y - 2);
        }
    }

    /** Draws the name in the title row while not editing; relative to the panel like the screen's labels. */
    public void drawTitle(GuiGraphicsExtractor graphics, Font font, int left, int top) {
        if (editing) {
            return;
        }
        Component text = shownTitle();
        int room = rowWidth - BUTTON - GAP;
        if (font.width(text) > room) {
            String cut = font.plainSubstrByWidth(text.getString(), room - font.width("...")) + "...";
            text = Component.literal(cut);
        }
        graphics.text(font, text, rowX - left, rowY - top, TITLE_COLOUR, false);
    }

    /** Whether the GUI's machine or site carries a name, typed or saved. */
    public boolean named() {
        String shown = editing && box != null ? box.getValue() : sentName;
        return !clean.apply(shown).isEmpty();
    }

    public boolean editing() {
        return editing;
    }

    /** A click on the pencil or check toggles; a click anywhere else while editing saves first. Returns whether it was taken. */
    public boolean mouseClicked(MouseButtonEvent event) {
        if (button != null && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && button.isMouseOver(event.x(), event.y())) {
            button.playDownSound(Minecraft.getInstance().getSoundManager());
            toggle();
            return true;
        }
        if (editing && !box.isMouseOver(event.x(), event.y())) {
            save();
        }
        return false;
    }

    /** While editing, the box keeps every key so E does not close the GUI; Enter saves and Escape cancels. */
    public boolean keyPressed(KeyEvent event) {
        if (!editing) {
            return false;
        }
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return box.isFocused() && (box.keyPressed(event) || box.canConsumeInput());
    }

    /** Saves when the box lost its focus some other way, such as another widget taking it. */
    public void tick() {
        if (editing && !box.isFocused()) {
            save();
        }
    }

    /** Saves a name still being typed when the GUI closes. */
    public void removed() {
        if (editing) {
            save();
        }
    }

    private void toggle() {
        if (editing) {
            save();
        } else {
            startEditing();
        }
    }

    private void startEditing() {
        editing = true;
        placeBox();
        box.setValue(sentName);
        box.moveCursorToStart(false);
        box.moveCursorToEnd(false);
        box.setVisible(true);
        button.refresh(true);
        focus.accept(box);
    }

    private void save() {
        String value = box.getValue();
        if (!clean.apply(value).equals(clean.apply(sentName))) {
            send.accept(value);
        }
        sentName = clean.apply(value);
        stopEditing();
    }

    /** The box scrolls its text by its width, so it takes its full width before it gets a value. */
    private void placeBox() {
        box.setPosition(rowX - 2, rowY - 2);
        box.setWidth(rowWidth - BUTTON - GAP + 2);
    }

    private void cancel() {
        box.setValue(sentName);
        stopEditing();
    }

    private void stopEditing() {
        editing = false;
        box.setVisible(false);
        box.setFocused(false);
        button.refresh(false);
        focus.accept(null);
    }

    private Component shownTitle() {
        return sentName.isEmpty() ? placeholder : Component.literal(sentName);
    }

    /** The small framed button of the title row: a pencil to rename, a green check to save. */
    private static final class EditButton extends AbstractButton {

        private static final int DARK = 0xFF373737;
        private static final int FACE = 0xFFC8C8C8;
        private static final int FACE_HOVER = 0xFFDDDDDD;
        private static final int LIGHT = 0xFFFFFFFF;
        private static final int OUTLINE = 0xFF3A3A3A;
        private static final int BODY = 0xFFE0A030;
        private static final int WOOD = 0xFFE8C890;
        private static final int ERASER = 0xFFD07070;
        private static final int CHECK = 0xFF2E8B3A;

        private final Runnable onPress;
        private boolean check;

        EditButton(Runnable onPress) {
            super(0, 0, BUTTON, BUTTON, Component.translatable("gui.servus_foundation.name.rename"));
            this.onPress = onPress;
        }

        void refresh(boolean editing) {
            check = editing;
            Component label = Component.translatable(editing ? "gui.servus_foundation.name.save" : "gui.servus_foundation.name.rename");
            setMessage(label);
            setTooltip(Tooltip.create(label));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            onPress.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            graphics.fill(x0, y0, x0 + width, y0 + height, DARK);
            graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + height - 1, isHovered() ? FACE_HOVER : FACE);
            graphics.fill(x0 + 1, y0 + 1, x0 + width - 1, y0 + 2, LIGHT);
            graphics.fill(x0 + 1, y0 + 1, x0 + 2, y0 + height - 1, LIGHT);
            if (check) {
                drawCheck(graphics, x0 + 2, y0 + 2);
            } else {
                drawPencil(graphics, x0 + 2, y0 + 2);
            }
        }

        /** A pencil 7 px square lying from bottom left (lead) to top right (eraser). */
        private static void drawPencil(GuiGraphicsExtractor graphics, int x, int y) {
            dot(graphics, x, y + 6, OUTLINE);
            dot(graphics, x + 1, y + 6, WOOD);
            dot(graphics, x, y + 5, WOOD);
            dot(graphics, x + 1, y + 5, WOOD);
            for (int i = 2; i <= 5; i++) {
                dot(graphics, x + i, y + 6 - i, BODY);
                dot(graphics, x + i - 1, y + 6 - i, BODY);
                dot(graphics, x + i, y + 7 - i, OUTLINE);
            }
            dot(graphics, x + 5, y, ERASER);
            dot(graphics, x + 6, y, ERASER);
            dot(graphics, x + 6, y + 1, ERASER);
        }

        /** A check mark 7 px wide and two pixels thick. */
        private static void drawCheck(GuiGraphicsExtractor graphics, int x, int y) {
            int[] rows = {3, 4, 5, 4, 3, 2, 1};
            for (int column = 0; column < rows.length; column++) {
                dot(graphics, x + column, y + rows[column], CHECK);
                dot(graphics, x + column, y + rows[column] + 1, CHECK);
            }
        }

        private static void dot(GuiGraphicsExtractor graphics, int x, int y, int colour) {
            graphics.fill(x, y, x + 1, y + 1, colour);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
