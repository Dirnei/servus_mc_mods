package io.github.dirnei.mc.servus.foundation.logistics.client;

import io.github.dirnei.mc.servus.foundation.client.NumberStepper;
import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * The amount popup of a duct filter slot: a title naming what the amount means
 * and the entry, a {@link NumberStepper} from 0, and Clear and Save. Saving an
 * empty field or 0 removes the amount; Escape closes without saving. The popup
 * grows with its stepper and buttons, so large mB amounts and steps fit.
 * Positions are absolute.
 */
final class AmountPopup {

    private static final int MIN_WIDTH = 110;
    private static final int MIN_BUTTON_WIDTH = 44;
    /** Room around a button label. */
    private static final int LABEL_PAD = 12;
    private static final int PAD = 6;
    private static final int TITLE_HEIGHT = 12;
    private static final int BUTTON_HEIGHT = 16;
    private static final int HEIGHT = PAD + TITLE_HEIGHT + NumberStepper.height() + PAD + BUTTON_HEIGHT + PAD;

    private final IntConsumer onSave;
    private NumberStepper stepper;
    private Component title = Component.empty();
    private int areaLeft;
    private int areaTop;
    private int areaWidth;
    private int areaHeight;
    private int popupWidth = MIN_WIDTH;
    private int buttonWidth = MIN_BUTTON_WIDTH;
    private int x;
    private int y;

    /** {@code onSave} hears the saved amount, 0 for none. */
    AmountPopup(IntConsumer onSave) {
        this.onSave = onSave;
    }

    /** The GUI area the popup centres on and dims. */
    void setArea(int left, int top, int width, int height) {
        areaLeft = left;
        areaTop = top;
        areaWidth = width;
        areaHeight = height;
        layout();
    }

    /** Centres the popup in the area at its current width and places the stepper. */
    private void layout() {
        x = areaLeft + (areaWidth - popupWidth) / 2;
        y = areaTop + (areaHeight - HEIGHT) / 2;
        if (stepper != null) {
            stepper.place(stepperX(), stepperY());
        }
    }

    /**
     * Sizes the popup to its content measured with the font: the stepper, Clear and Save, and the title, at
     * least {@link #MIN_WIDTH} and at most the area. The stepper and buttons always fit; a longer title is cut.
     */
    private void fit(Font font) {
        buttonWidth = Math.max(MIN_BUTTON_WIDTH, Math.max(font.width(Component.translatable("gui.servus_foundation.amount.clear")),
                font.width(Component.translatable("gui.servus_foundation.amount.save"))) + LABEL_PAD);
        int content = Math.max(stepper.width(), 2 * buttonWidth + PAD);
        int needed = Math.max(content, font.width(title)) + 2 * PAD;
        popupWidth = Math.max(MIN_WIDTH, Math.max(content + 2 * PAD, Math.min(needed, areaWidth)));
    }

    boolean isOpen() {
        return stepper != null;
    }

    /** Opens with the current amount, the largest amount and the step sizes from the field outwards. */
    void open(Component title, int amount, int max, int... steps) {
        this.title = title;
        Font font = Minecraft.getInstance().font;
        stepper = new NumberStepper(font, title, 0, max, () -> 1, value -> { }, steps);
        stepper.setValue(amount);
        fit(font);
        layout();
        stepper.field().setFocused(true);
    }

    void close() {
        stepper = null;
    }

    void tick() {
        if (stepper != null) {
            stepper.tick();
        }
    }

    private int stepperX() {
        return x + (popupWidth - stepper.width()) / 2;
    }

    private int stepperY() {
        return y + PAD + TITLE_HEIGHT;
    }

    private int buttonsY() {
        return y + HEIGHT - PAD - BUTTON_HEIGHT;
    }

    private int clearX() {
        return x + PAD;
    }

    private int saveX() {
        return x + popupWidth - PAD - buttonWidth;
    }

    void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (stepper == null) {
            return;
        }
        PopupPanel.draw(graphics, areaLeft, areaTop, areaWidth, areaHeight, x, y, popupWidth, HEIGHT);
        graphics.text(font, font.plainSubstrByWidth(title.getString(), popupWidth - 2 * PAD), x + PAD, y + PAD, PopupButtons.TEXT, false);
        stepper.render(graphics, mouseX, mouseY, partialTick);
        PopupButtons.button(graphics, font, clearX(), buttonsY(), buttonWidth, BUTTON_HEIGHT,
                Component.translatable("gui.servus_foundation.amount.clear"),
                PopupButtons.over(mouseX, mouseY, clearX(), buttonsY(), buttonWidth, BUTTON_HEIGHT), true);
        PopupButtons.button(graphics, font, saveX(), buttonsY(), buttonWidth, BUTTON_HEIGHT,
                Component.translatable("gui.servus_foundation.amount.save"),
                PopupButtons.over(mouseX, mouseY, saveX(), buttonsY(), buttonWidth, BUTTON_HEIGHT), true);
    }

    /** Steps, focuses the field, clears, saves, or closes on a click outside; every click is used while open. */
    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (stepper == null) {
            return false;
        }
        for (AbstractWidget widget : stepper.widgets()) {
            if (widget.mouseClicked(event, doubleClick)) {
                stepper.field().setFocused(widget == stepper.field());
                return true;
            }
        }
        if (PopupButtons.over(event.x(), event.y(), clearX(), buttonsY(), buttonWidth, BUTTON_HEIGHT)) {
            save(0);
        } else if (PopupButtons.over(event.x(), event.y(), saveX(), buttonsY(), buttonWidth, BUTTON_HEIGHT)) {
            save(fieldAmount());
        } else if (!PopupButtons.over(event.x(), event.y(), x, y, popupWidth, HEIGHT)) {
            close();
        }
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (stepper == null) {
            return false;
        }
        stepper.mouseScrolled(mouseX, mouseY, scrollY);
        return true;
    }

    /** Escape closes, Enter saves; every other key goes to the field while open. */
    boolean keyPressed(KeyEvent event) {
        if (stepper == null) {
            return false;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
        } else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            save(fieldAmount());
        } else {
            stepper.field().keyPressed(event);
        }
        return true;
    }

    boolean charTyped(CharacterEvent event) {
        if (stepper == null) {
            return false;
        }
        stepper.field().charTyped(event);
        return true;
    }

    private int fieldAmount() {
        return stepper.field().getValue().isEmpty() ? 0 : stepper.value();
    }

    private void save(int amount) {
        close();
        onSave.accept(amount);
    }
}
