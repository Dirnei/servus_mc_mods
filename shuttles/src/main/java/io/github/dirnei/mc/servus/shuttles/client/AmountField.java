package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.field.FieldAmount;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongToIntFunction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * A digits only number field for a value a block keeps, such as a
 * {@link FieldAmount}. It sends its number, limited by {@code clamp}, when it
 * loses focus or {@link #commit()} is called, and otherwise shows the value
 * the block keeps, but never while the player types in it. The owning screen
 * calls {@link #tick()} every client tick.
 */
final class AmountField {

    /** Up to six digits, so a number above the largest level can be typed and is clamped. */
    private static final int MAX_LENGTH = 6;
    /** Ticks a sent value waits for the server's answer before the head's value shows again. */
    private static final int ANSWER_TICKS = 20;

    private final EditBox box;
    private final LongToIntFunction clamp;
    private final IntSupplier headValue;
    private final LongConsumer send;
    private boolean wasFocused;
    /** The value last sent while the head's value has not caught up; -1 for none. */
    private int sent = -1;
    private int answerTicks;

    AmountField(Font font, int x, int y, int width, int height, LongToIntFunction clamp, Component name, IntSupplier headValue,
                LongConsumer send) {
        this.clamp = clamp;
        this.headValue = headValue;
        this.send = send;
        this.box = new EditBox(font, x, y, width, height, name);
        box.setMaxLength(MAX_LENGTH);
        box.setFilter(text -> text.chars().allMatch(c -> c >= '0' && c <= '9'));
        box.setValue(String.valueOf(headValue.getAsInt()));
    }

    EditBox box() {
        return box;
    }

    boolean isFocused() {
        return box.isFocused();
    }

    /** Sends the number when the box lost focus, and shows the head's value while it has none. */
    void tick() {
        boolean focused = box.isFocused();
        if (wasFocused && !focused) {
            commit();
        }
        wasFocused = focused;
        if (!focused) {
            showHeadValue();
        }
    }

    /** Clamps the typed number, shows it and sends it when it differs from the head's value. */
    void commit() {
        wasFocused = false;
        int value = clamp.applyAsInt(FieldAmount.parse(box.getValue()));
        box.setValue(String.valueOf(value));
        if (value != headValue.getAsInt()) {
            send.accept(value);
            sent = value;
            answerTicks = ANSWER_TICKS;
        }
    }

    private void showHeadValue() {
        int head = headValue.getAsInt();
        if (sent >= 0 && head != sent && answerTicks-- > 0) {
            return;
        }
        sent = -1;
        String text = String.valueOf(head);
        if (!text.equals(box.getValue())) {
            box.setValue(text);
        }
    }
}
