package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A number field with a row of plus step buttons above it and the matching
 * minus buttons below it. A step of {@link AmountSteps#STACK} snaps to full
 * stacks of the size the stack supplier gives and is labelled with it. The
 * buttons grow to fit their widest label and the field to fit the largest
 * value, measured with the font, so {@link #width()} depends on both.
 *
 * <p>Use: create it in {@code init}, add every widget of {@link #widgets()}
 * with {@code addRenderableWidget}, {@link #place} it with absolute
 * coordinates, call {@link #tick()} from {@code containerTick}, forward the
 * mouse wheel to {@link #mouseScrolled} and key presses to the {@link #field()}.
 * A screen that draws a panel over its widgets calls {@link #render} after the
 * panel. The callback hears every new value: a step, a wheel turn or typing.
 */
public final class NumberStepper {

    /** The narrowest step button; wider labels widen every button of the stepper. */
    public static final int BUTTON_WIDTH = 24;
    public static final int BUTTON_HEIGHT = 14;
    /** The narrowest field; a larger maximum widens it. */
    public static final int FIELD_WIDTH = 48;
    private static final int GAP = 1;
    private static final int ROW = 16;
    /** Room around a button label. */
    private static final int LABEL_PAD = 6;
    /** Room inside the field's frame around its text. */
    private static final int FIELD_PAD = 10;
    /** The widest stack size a stack step can show. */
    private static final String WIDEST_STACK = "99";

    private final int buttonWidth;
    private final EditBox field;
    private final List<Button> plus = new ArrayList<>();
    private final List<Button> minus = new ArrayList<>();
    private final int[] steps;
    private final IntSupplier stack;
    private final int min;
    private final int max;
    private final IntConsumer changed;
    private int value;
    private boolean wasFocused;

    /**
     * @param steps the step sizes from the field outwards, positive; {@link AmountSteps#STACK} for a stack step
     */
    public NumberStepper(Font font, Component name, int min, int max, IntSupplier stack, IntConsumer changed, int... steps) {
        this.min = min;
        this.max = max;
        this.stack = stack;
        this.changed = changed;
        this.steps = steps.clone();
        this.value = min;
        this.buttonWidth = buttonWidth(font, this.steps);
        int fieldWidth = Math.max(FIELD_WIDTH, font.width(Integer.toString(max)) + FIELD_PAD);
        field = new EditBox(font, 0, 0, fieldWidth, BUTTON_HEIGHT, name);
        field.setMaxLength(Integer.toString(max).length() + 1);
        field.setFilter(text -> text.chars().allMatch(Character::isDigit));
        field.setValue(Integer.toString(value));
        field.setResponder(this::typed);
        for (int step : this.steps) {
            plus.add(Button.builder(Component.empty(), b -> step(step)).size(buttonWidth, BUTTON_HEIGHT).build());
            minus.add(Button.builder(Component.empty(), b -> step(-step)).size(buttonWidth, BUTTON_HEIGHT).build());
        }
        relabel();
    }

    /** The width every step button needs so its widest label, plus or minus, fits with room around it. */
    private static int buttonWidth(Font font, int[] steps) {
        int widest = 0;
        for (int step : steps) {
            String text = step == AmountSteps.STACK ? WIDEST_STACK : Integer.toString(step);
            widest = Math.max(widest, Math.max(font.width("+" + text), font.width("-" + text)));
        }
        return Math.max(BUTTON_WIDTH, widest + LABEL_PAD);
    }

    /** The field and every step button, for {@code addRenderableWidget}. */
    public List<AbstractWidget> widgets() {
        List<AbstractWidget> widgets = new ArrayList<>(plus);
        widgets.add(field);
        widgets.addAll(minus);
        return widgets;
    }

    public EditBox field() {
        return field;
    }

    /** Width of a button row, or of the field when that is wider; the field starts at the left edge of the rows. */
    public int width() {
        return Math.max(steps.length * (buttonWidth + GAP) - GAP, field.getWidth());
    }

    public static int height() {
        return 2 * ROW + BUTTON_HEIGHT;
    }

    /** Puts the plus row at {@code y}, the field one row below and the minus row below that. */
    public void place(int x, int y) {
        for (int i = 0; i < steps.length; i++) {
            int buttonX = x + i * (buttonWidth + GAP);
            plus.get(i).setPosition(buttonX, y);
            minus.get(i).setPosition(buttonX, y + 2 * ROW);
        }
        field.setPosition(x, y + ROW);
    }

    public void setVisible(boolean visible) {
        widgets().forEach(widget -> widget.visible = visible);
    }

    public int value() {
        return value;
    }

    /** Shows {@code amount}, kept in range, without telling the callback. */
    public void setValue(int amount) {
        value = clamp(amount);
        field.setValue(Integer.toString(value));
        relabel();
    }

    /** Rewrites the field with the clamped value, as on Enter or when it loses focus. */
    public void commit() {
        field.setValue(Integer.toString(value));
    }

    /** Commits when the field lost focus and follows a changed stack size on the labels. */
    public void tick() {
        boolean focused = field.isFocused();
        if (wasFocused && !focused) {
            commit();
        }
        wasFocused = focused;
        relabel();
    }

    /** Steps by 1 for a wheel turn over the field; true when it was over the field. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!field.visible || !field.isMouseOver(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        step(scrollY > 0 ? 1 : -1);
        return true;
    }

    /** Draws the widgets again, for a screen that drew a panel over them. */
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        for (AbstractWidget widget : widgets()) {
            if (widget.visible) {
                widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    private void step(int step) {
        set(AmountSteps.step(value, step, stack.getAsInt(), min, max));
        commit();
    }

    private void typed(String text) {
        if (text.isEmpty()) {
            return;
        }
        try {
            set(clamp(Long.parseLong(text)));
        } catch (NumberFormatException e) {
            set(max);
        }
    }

    private void set(int next) {
        if (next != value) {
            value = next;
            changed.accept(value);
        }
    }

    private int clamp(long amount) {
        return (int) Math.max(min, Math.min(max, amount));
    }

    private void relabel() {
        int size = Math.max(1, stack.getAsInt());
        for (int i = 0; i < steps.length; i++) {
            String step = steps[i] == AmountSteps.STACK ? Integer.toString(size) : Integer.toString(steps[i]);
            plus.get(i).setMessage(Component.literal("+" + step));
            minus.get(i).setMessage(Component.literal("-" + step));
        }
    }
}
