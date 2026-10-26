package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ui.RecipeGridLayout;
import io.github.dirnei.mc.servus.foundation.ui.RecipeGridLayout.Box;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The recipe grid view shared by every screen that edits or orders a 3 by 3
 * crafting grid: a popup with an optional title, the frames of the ghost
 * cells (the menu owns the slots, placed with
 * {@link io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons}),
 * an arrow, the result as a bare icon with its count and tooltip, the mode
 * button and a row of actions the screen supplies. It knows nothing of
 * patterns: a {@link Source} says what the result and the mode button show.
 *
 * <p>Use: create it with the popup's place relative to the GUI, add every
 * widget of {@link #build} with {@code addRenderableWidget}, call
 * {@link #setVisible} when the popup opens or closes, {@link #render} where
 * the screen draws its popups and {@link #renderTooltips} after its widgets.
 * A screen that draws the popup over its slots and widgets also calls
 * {@link #renderCells} and redraws {@link #widgets()}. Forward presses and
 * drags to {@link #press} and {@link #drag}, which fill cells by dragging the
 * cursor's item over them. Client only.
 */
public final class RecipeGridView {

    /** What the mode button shows: its label, whether it can be pressed and its tooltip. */
    public record ModeState(Component label, boolean enabled, List<Component> tooltip) {
    }

    /** What the grid shows, worked out by the screen from its menu. */
    public interface Source {
        /** The result of one craft, empty while the cells match nothing. */
        ItemStack result();

        /** Lines added under the result's own tooltip. */
        default List<Component> resultNotes() {
            return List.of();
        }

        ModeState mode();

        /** Called when the enabled mode button is pressed. */
        void toggleMode();
    }

    /**
     * One button of the action row.
     *
     * @param shown   whether the button is on the row now; the shown ones are spread over the row
     * @param refusal the lang key why it cannot be pressed now, null while it can
     */
    public record Action(Component label, BooleanSupplier shown, Supplier<String> refusal, Runnable press) {

        /** An action that is always on the row and can always be pressed. */
        public static Action of(Component label, Runnable press) {
            return new Action(label, () -> true, () -> null, press);
        }

        /** Cancel, always on the row. */
        public static Action cancel(Runnable press) {
            return of(Component.translatable("gui.servus_foundation.recipe_grid.cancel"), press);
        }

        /** Clear, always on the row. */
        public static Action clear(Runnable press) {
            return of(Component.translatable("gui.servus_foundation.recipe_grid.clear"), press);
        }

        /** An action that cannot be pressed while the grid's result is empty. */
        public static Action needsResult(Component label, Source source, BooleanSupplier shown, Runnable press) {
            return new Action(label, shown, () -> source.result().isEmpty() ? NO_MATCH : null, press);
        }
    }

    /** Why an action that needs a result cannot be pressed. */
    public static final String NO_MATCH = "gui.servus_foundation.recipe_grid.no_match";
    private static final int ARROW_COLOUR = 0xFF8B8B8B;
    private static final int TITLE_COLOUR = 0xFF404040;
    private static final int CELL_BORDER = 0xFF373737;
    private static final int CELL_FACE = 0xFF8B8B8B;
    private static final int CELL_HOVER = 0x80FFFFFF;

    private final int x;
    private final int y;
    private final Supplier<Component> title;
    private final boolean titled;
    private final Source source;
    private final List<Action> actions;
    private final List<Button> actionButtons = new ArrayList<>();
    private final String[] refusals;
    private final Set<Integer> dragged = new HashSet<>();
    private Button modeButton;
    private int left;
    private int top;
    private boolean visible;

    /**
     * @param x       popup left edge, relative to the GUI
     * @param y       popup top edge, relative to the GUI
     * @param title   the title row's text, or null for a popup without one
     * @param actions the action row, left to right; at most {@link RecipeGridLayout#MAX_ACTIONS} shown at once
     */
    public RecipeGridView(int x, int y, Supplier<Component> title, Source source, List<Action> actions) {
        this.x = x;
        this.y = y;
        this.title = title;
        this.titled = title != null;
        this.source = source;
        this.actions = List.copyOf(actions);
        this.refusals = new String[actions.size()];
    }

    /** Creates the mode button and the action buttons at the GUI's place; the screen adds them as widgets. */
    public List<AbstractWidget> build(int guiLeft, int guiTop) {
        left = guiLeft;
        top = guiTop;
        Box mode = box(RecipeGridLayout.mode(titled));
        modeButton = Button.builder(Component.empty(), b -> source.toggleMode()).bounds(mode.x(), mode.y(), mode.w(), mode.h()).build();
        actionButtons.clear();
        for (Action action : actions) {
            actionButtons.add(Button.builder(action.label(), b -> action.press().run())
                    .size(RecipeGridLayout.ACTION_WIDTH, RecipeGridLayout.ACTIONS.h()).build());
        }
        Arrays.fill(refusals, null);
        update();
        return widgets();
    }

    /** The view's buttons, for screens that route presses or redraw widgets over their popup themselves. */
    public List<AbstractWidget> widgets() {
        List<AbstractWidget> widgets = new ArrayList<>();
        if (modeButton != null) {
            widgets.add(modeButton);
        }
        widgets.addAll(actionButtons);
        return widgets;
    }

    /** Shows or hides the view's buttons with the popup. */
    public void setVisible(boolean open) {
        visible = open;
        if (!open) {
            dragged.clear();
        }
        update();
    }

    /** The place beside the result for a widget of the screen's own, in screen coordinates. */
    public Box extra() {
        return box(RecipeGridLayout.extra(titled));
    }

    /** The ghost cell under the mouse while the view shows, else -1. */
    public int cellAt(double mouseX, double mouseY) {
        return visible ? RecipeGridLayout.cellAt(mouseX - left - x, mouseY - top - y, titled) : -1;
    }

    /**
     * A press: starts a new drag, and with an item on the cursor sets the cell
     * under the mouse through {@code click}.
     *
     * @return whether the press set a cell, so the screen takes its release too
     */
    public boolean press(double mouseX, double mouseY, boolean carrying, IntConsumer click) {
        dragged.clear();
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0) {
            return false;
        }
        dragged.add(cell);
        if (!carrying) {
            return false;
        }
        click.accept(cell);
        return true;
    }

    /**
     * A drag with an item on the cursor sets each cell it passes once.
     *
     * @return whether the drag is over a cell and taken
     */
    public boolean drag(double mouseX, double mouseY, boolean carrying, IntConsumer click) {
        int cell = cellAt(mouseX, mouseY);
        if (cell < 0 || !carrying) {
            return false;
        }
        if (dragged.add(cell)) {
            click.accept(cell);
        }
        return true;
    }

    /**
     * The popup over the dimmed area, its title, the cell frames, the arrow and
     * the result; updates the buttons. All coordinates are absolute.
     */
    public void render(GuiGraphicsExtractor graphics, Font font, int dimX, int dimY, int dimWidth, int dimHeight) {
        Box popup = box(RecipeGridLayout.POPUP);
        PopupPanel.draw(graphics, dimX, dimY, dimWidth, dimHeight, popup.x(), popup.y(), popup.w(), popup.h());
        if (titled) {
            Box row = box(RecipeGridLayout.TITLE);
            graphics.text(font, font.plainSubstrByWidth(title.get().getString(), row.w()), row.x(), row.y(), TITLE_COLOUR, false);
        }
        for (int cell = 0; cell < 9; cell++) {
            int cx = cellX(cell);
            int cy = cellY(cell);
            graphics.fill(cx - 1, cy - 1, cx + 17, cy + 17, CELL_BORDER);
            graphics.fill(cx, cy, cx + 16, cy + 16, CELL_FACE);
        }
        Box arrow = box(RecipeGridLayout.arrow(titled));
        graphics.fill(arrow.x(), arrow.y() + 3, arrow.x() + 10, arrow.y() + 7, ARROW_COLOUR);
        for (int i = 0; i < 5; i++) {
            graphics.fill(arrow.x() + 10 + i, arrow.y() + i, arrow.x() + 11 + i, arrow.y() + 10 - i, ARROW_COLOUR);
        }
        ItemStack result = source.result();
        if (!result.isEmpty()) {
            Box at = box(RecipeGridLayout.result(titled));
            graphics.item(result, at.x(), at.y());
            graphics.itemDecorations(font, result, at.x(), at.y());
        }
        update();
    }

    /** The ghost cells' items with the hover shade, for screens that draw the popup over the menu's slots. */
    public void renderCells(GuiGraphicsExtractor graphics, List<ItemStack> cells, int mouseX, int mouseY) {
        for (int cell = 0; cell < cells.size() && cell < 9; cell++) {
            int cx = cellX(cell);
            int cy = cellY(cell);
            graphics.item(cells.get(cell), cx, cy);
            if (mouseX >= cx && mouseX < cx + 16 && mouseY >= cy && mouseY < cy + 16) {
                graphics.fill(cx, cy, cx + 16, cy + 16, CELL_HOVER);
            }
        }
    }

    /** The result's tooltip with the source's notes, and the mode button's tooltip, while hovered. */
    public void renderTooltips(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (!visible) {
            return;
        }
        ItemStack result = source.result();
        if (!result.isEmpty() && box(RecipeGridLayout.result(titled)).contains(mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), result));
            lines.addAll(source.resultNotes());
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        } else if (box(RecipeGridLayout.mode(titled)).contains(mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(font, source.mode().tooltip(), mouseX, mouseY);
        }
    }

    private void update() {
        if (modeButton == null) {
            return;
        }
        ModeState mode = source.mode();
        modeButton.visible = visible;
        modeButton.active = mode.enabled();
        modeButton.setMessage(mode.label());
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) {
            boolean on = visible && actions.get(i).shown().getAsBoolean();
            actionButtons.get(i).visible = on;
            if (on) {
                shown.add(i);
            }
        }
        List<Box> places = RecipeGridLayout.actions(shown.size());
        for (int i = 0; i < shown.size(); i++) {
            int index = shown.get(i);
            Box place = box(places.get(i));
            Button button = actionButtons.get(index);
            button.setPosition(place.x(), place.y());
            refusals[index] = GreyOut.apply(button, refusals[index], actions.get(index).refusal().get());
        }
    }

    private int cellX(int cell) {
        return left + x + RecipeGridLayout.cellX(cell);
    }

    private int cellY(int cell) {
        return top + y + RecipeGridLayout.cellY(cell, titled);
    }

    private Box box(Box relative) {
        return relative.offset(left + x, top + y);
    }
}
