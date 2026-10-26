package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * A one line number stepper: a minus button, a digits only field and a plus
 * button side by side, one button high. A click steps by 1, a shift click by
 * a full stack of the size the stack supplier gives; the wheel over the field
 * steps by 1. Typing is clamped on Enter ({@link #commit()}) or when the field
 * loses focus.
 *
 * <p>Use: create it in {@code init}, add every widget of {@link #widgets()}
 * with {@code addRenderableWidget}, {@link #place} it with absolute
 * coordinates, call {@link #tick()} from {@code containerTick}, forward the
 * mouse wheel to {@link #mouseScrolled} and key presses to the {@link #field()}.
 * A screen that draws a panel over its widgets calls {@link #render} after the
 * panel. The callback hears every new value.
 */
public final class InlineStepper {

    public static final int BUTTON_WIDTH = 14;
    public static final int HEIGHT = 14;
    private static final int GAP = 1;

    private final EditBox field;
    private final Button minus;
    private final Button plus;
    private final IntSupplier stack;
    private final int min;
    private final int max;
    private final IntConsumer changed;
    private int value;
    private boolean wasFocused;
    /** Whether a value means off: the field is then empty and shows its hint. */
    private boolean offShown;
    private int offValue;

    public InlineStepper(Font font, Component name, int min, int max, IntSupplier stack, IntConsumer changed) {
        this.min = min;
        this.max = max;
        this.stack = stack;
        this.changed = changed;
        this.value = min;
        field = new EditBox(font, 0, 0, BUTTON_WIDTH, HEIGHT, name);
        field.setMaxLength(Integer.toString(max).length() + 1);
        field.setFilter(text -> text.chars().allMatch(Character::isDigit));
        field.setValue(Integer.toString(value));
        field.setResponder(this::typed);
        minus = Button.builder(Component.literal("-"), b -> step(false)).size(BUTTON_WIDTH, HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.servus_foundation.stepper.less"))).build();
        plus = Button.builder(Component.literal("+"), b -> step(true)).size(BUTTON_WIDTH, HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.servus_foundation.stepper.more"))).build();
    }

    /**
     * Makes {@code value} mean off: the field shows {@code hint} instead of
     * it, and clearing the field turns it off. The value must lie in range,
     * usually the lowest one.
     */
    public InlineStepper offAt(int value, Component hint) {
        offShown = true;
        offValue = clamp(value);
        field.setHint(hint);
        commit();
        return this;
    }

    /** The minus button, the field and the plus button, for {@code addRenderableWidget}. */
    public List<AbstractWidget> widgets() {
        return List.of(minus, field, plus);
    }

    public EditBox field() {
        return field;
    }

    /** Puts the row at {@code x}, {@code y}, {@code width} wide: the field takes what the two buttons leave. */
    public void place(int x, int y, int width) {
        int fieldWidth = Math.max(BUTTON_WIDTH, width - 2 * (BUTTON_WIDTH + GAP));
        minus.setPosition(x, y);
        field.setPosition(x + BUTTON_WIDTH + GAP, y);
        field.setWidth(fieldWidth);
        plus.setPosition(x + BUTTON_WIDTH + GAP + fieldWidth + GAP, y);
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
        commit();
    }

    /** Rewrites the field with the clamped value, as on Enter or when it loses focus. */
    public void commit() {
        field.setValue(offShown && value == offValue ? "" : Integer.toString(value));
    }

    /** Commits when the field lost focus. */
    public void tick() {
        boolean focused = field.isFocused();
        if (wasFocused && !focused) {
            commit();
        }
        wasFocused = focused;
    }

    /** Steps by 1 for a wheel turn over the field; true when it was over the field. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!field.visible || !field.isMouseOver(mouseX, mouseY) || scrollY == 0) {
            return false;
        }
        set(AmountSteps.step(value, scrollY > 0 ? 1 : -1, stack.getAsInt(), min, max));
        commit();
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

    private void step(boolean up) {
        int size = Minecraft.getInstance().hasShiftDown() ? AmountSteps.STACK : 1;
        set(AmountSteps.step(value, up ? size : -size, stack.getAsInt(), min, max));
        commit();
    }

    private void typed(String text) {
        if (text.isEmpty()) {
            if (offShown) {
                set(offValue);
            }
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
}
