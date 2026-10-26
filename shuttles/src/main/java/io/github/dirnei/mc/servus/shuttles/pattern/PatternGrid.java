package io.github.dirnei.mc.servus.shuttles.pattern;

import io.github.dirnei.mc.servus.foundation.block.RecipeEditorButtons;
import io.github.dirnei.mc.servus.foundation.logistics.menu.GhostSlot;
import io.github.dirnei.mc.servus.foundation.transfer.ItemSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The 3 by 3 ghost grid and the Exact or Any mode of a menu that edits or
 * orders a crafting pattern in the recipe grid view: the Production
 * Terminal's editor and the Storage Terminal's order grid. The menu adds
 * {@link #slots} and {@link #modeSlot()}, hands its grid clicks to
 * {@link #setGhost} and its menu buttons to {@link #button}. Works on both
 * sides; the cells and the mode reach the client by the menu's slot and data sync.
 */
public final class PatternGrid {

    private final ItemSlots cells = new ItemSlots(EncodedPattern.GRID);
    private final DataSlot mode = DataSlot.standalone();

    public PatternGrid() {
        resetMode();
    }

    /** The nine ghost slots at a grid whose first item origin is {@code x, y}, there while {@code active} holds. */
    public List<Slot> slots(int x, int y, BooleanSupplier active) {
        List<Slot> slots = new ArrayList<>(EncodedPattern.GRID);
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            slots.add(new GhostSlot(() -> cells, cell, x + (cell % 3) * 18, y + (cell / 3) * 18) {
                @Override
                public boolean isActive() {
                    return active.getAsBoolean();
                }
            });
        }
        return slots;
    }

    /** The mode for the menu's data sync. */
    public DataSlot modeSlot() {
        return mode;
    }

    /** The mode asked for; {@link #pattern} stores Exact where Any is not possible. */
    public EncodedPattern.Mode mode() {
        return mode.get() == EncodedPattern.Mode.ANY.ordinal() ? EncodedPattern.Mode.ANY : EncodedPattern.Mode.EXACT;
    }

    public void setMode(EncodedPattern.Mode asked) {
        mode.set(asked.ordinal());
    }

    /** Switches the asked mode between Exact and Any. */
    public void toggleMode() {
        setMode(mode() == EncodedPattern.Mode.ANY ? EncodedPattern.Mode.EXACT : EncodedPattern.Mode.ANY);
    }

    /** Asks for Any again, as a new grid does. */
    public void resetMode() {
        setMode(EncodedPattern.Mode.ANY);
    }

    /** Puts a single copy of the stack into a cell, or clears it for an empty stack. */
    public void setGhost(int cell, ItemStack stack) {
        cells.setStackInSlot(cell, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
    }

    /** The cells, empty stacks for empty cells. */
    public List<ItemStack> cells() {
        List<ItemStack> list = new ArrayList<>(EncodedPattern.GRID);
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            list.add(cells.getStackInSlot(cell));
        }
        return list;
    }

    /** Sets every cell from the template, cells past its end empty. */
    public void fill(List<ItemStack> template) {
        for (int cell = 0; cell < EncodedPattern.GRID; cell++) {
            setGhost(cell, cell < template.size() ? template.get(cell) : ItemStack.EMPTY);
        }
    }

    public void clear() {
        fill(List.of());
    }

    /**
     * Handles the grid's own menu buttons, Mode and Clear.
     *
     * @return whether the button was one of them
     */
    public boolean button(int id) {
        if (id == RecipeEditorButtons.MODE) {
            toggleMode();
            return true;
        }
        if (id == RecipeEditorButtons.CLEAR) {
            clear();
            return true;
        }
        return false;
    }

    /** The pattern the cells make with the asked mode, Exact where Any is not possible; empty when they match nothing. */
    public Optional<EncodedPattern> pattern(Level level) {
        return PatternEditing.encode(level, cells()).map(pattern -> pattern.withMode(PatternEditing.allowed(level, pattern, mode())));
    }
}
