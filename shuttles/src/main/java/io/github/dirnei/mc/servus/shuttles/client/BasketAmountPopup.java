package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.foundation.client.NumberStepper;
import io.github.dirnei.mc.servus.foundation.client.PopupPanel;
import io.github.dirnei.mc.servus.foundation.ui.AmountSteps;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The amount popup of the handheld's basket: the item, a {@link NumberStepper}
 * and two buttons. Opened to add an item it offers Cancel and Add; opened on a
 * basket entry it offers Remove and Save. Enter confirms, Escape or a click
 * outside closes. Positions are absolute.
 */
final class BasketAmountPopup {

    private static final String KEY = "gui.servus_shuttles.handheld.";
    private static final int MIN_WIDTH = 120;
    private static final int PAD = 6;
    private static final int TITLE_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 16;
    private static final int HEIGHT = PAD + TITLE_HEIGHT + NumberStepper.height() + PAD + BUTTON_HEIGHT + PAD;
    private static final int TEXT = 0xFF202020;

    /** Hears Add or Save: the item, the amount, and whether the popup edited an entry already in the basket. */
    interface Confirm {
        void accept(ItemStack item, int amount, boolean editing);
    }

    private final Confirm onConfirm;
    private final Consumer<ItemStack> onRemove;
    private ItemStack item = ItemStack.EMPTY;
    private boolean editing;
    private NumberStepper stepper;
    private Button left;
    private Button right;
    private int areaX;
    private int areaY;
    private int areaWidth;
    private int areaHeight;
    private int x;
    private int y;
    private int width = MIN_WIDTH;

    /**
     * @param onConfirm hears Add or Save
     * @param onRemove  hears the item of Remove
     */
    BasketAmountPopup(Confirm onConfirm, Consumer<ItemStack> onRemove) {
        this.onConfirm = onConfirm;
        this.onRemove = onRemove;
    }

    /** The GUI area the popup centres on and dims. */
    void setArea(int areaX, int areaY, int areaWidth, int areaHeight) {
        this.areaX = areaX;
        this.areaY = areaY;
        this.areaWidth = areaWidth;
        this.areaHeight = areaHeight;
    }

    boolean isOpen() {
        return stepper != null;
    }

    /** Opens on {@code amount} of the item; {@code editing} for a basket entry, else to add it. */
    void open(Font font, ItemStack stack, int amount, boolean editing) {
        item = stack.copyWithCount(1);
        this.editing = editing;
        stepper = new NumberStepper(font, item.getHoverName(), AmountSteps.MIN, AmountSteps.MAX, item::getMaxStackSize, value -> { },
                1, 10, AmountSteps.STACK);
        stepper.setValue(amount);
        left = Button.builder(Component.translatable(KEY + (editing ? "basket_remove" : "basket_cancel")), b -> pressLeft())
                .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        right = Button.builder(Component.translatable(KEY + (editing ? "basket_save" : "basket_add")), b -> confirm())
                .size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        width = Math.max(MIN_WIDTH, Math.max(stepper.width(), 2 * BUTTON_WIDTH + PAD) + 2 * PAD);
        x = areaX + (areaWidth - width) / 2;
        y = areaY + (areaHeight - HEIGHT) / 2;
        stepper.place(x + (width - stepper.width()) / 2, y + PAD + TITLE_HEIGHT);
        left.setPosition(x + PAD, y + HEIGHT - PAD - BUTTON_HEIGHT);
        right.setPosition(x + width - PAD - BUTTON_WIDTH, y + HEIGHT - PAD - BUTTON_HEIGHT);
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

    private List<AbstractWidget> widgets() {
        List<AbstractWidget> widgets = new ArrayList<>(stepper.widgets());
        widgets.add(left);
        widgets.add(right);
        return widgets;
    }

    void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (stepper == null) {
            return;
        }
        PopupPanel.draw(graphics, areaX, areaY, areaWidth, areaHeight, x, y, width, HEIGHT);
        graphics.item(item, x + PAD, y + PAD);
        Component title = Component.translatable(KEY + (editing ? "basket_edit_title" : "basket_add_title"), item.getHoverName());
        graphics.text(font, font.plainSubstrByWidth(title.getString(), width - 3 * PAD - 16), x + PAD + 20, y + PAD + 4, TEXT, false);
        for (AbstractWidget widget : widgets()) {
            widget.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Steps, presses a button, focuses the field, or closes on a click outside; every click is used while open. */
    boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (stepper == null) {
            return false;
        }
        for (AbstractWidget widget : widgets()) {
            if (widget.mouseClicked(event, doubleClick)) {
                if (stepper != null) {
                    stepper.field().setFocused(widget == stepper.field());
                }
                return true;
            }
        }
        if (!(event.x() >= x && event.x() < x + width && event.y() >= y && event.y() < y + HEIGHT)) {
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

    /** Escape closes, Enter confirms; every other key goes to the field while open. */
    boolean keyPressed(KeyEvent event) {
        if (stepper == null) {
            return false;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            close();
        } else if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            confirm();
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

    private void confirm() {
        stepper.commit();
        int amount = stepper.value();
        close();
        onConfirm.accept(item, amount, editing);
    }

    private void pressLeft() {
        close();
        if (editing) {
            onRemove.accept(item);
        }
    }
}
