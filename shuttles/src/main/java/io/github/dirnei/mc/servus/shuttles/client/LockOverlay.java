package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.lock.SlotLocksPayload;
import io.github.dirnei.mc.servus.shuttles.menu.MenuLocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Draws slot locks on a container screen: a wash over each locked slot, one
 * colour for slots a job fills and one for slots it empties, the incoming
 * item faded in an empty slot a job fills, and a tooltip naming the job.
 */
public final class LockOverlay {

    private static final int WASH_IN = 0x6040A0FF;
    private static final int WASH_OUT = 0x60FF9A2E;
    /** Over the faded incoming item: strong enough that it reads as a ghost. */
    private static final int WASH_GHOST = 0x9040A0FF;

    private LockOverlay() {
    }

    /** The wash over a slot whose first lock is IN, else OUT. */
    public static int wash(boolean in) {
        return in ? WASH_IN : WASH_OUT;
    }

    /** Draws the washes and ghosts; call from {@code extractSlots} after the slots, in menu coordinates. */
    public static void draw(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, MenuLocks locks) {
        for (Slot slot : menu.slots) {
            List<SlotLocksPayload.Entry> entries = locks.shownAt(slot.index);
            if (entries.isEmpty()) {
                continue;
            }
            SlotLocksPayload.Entry first = entries.get(0);
            int colour = first.in() ? WASH_IN : WASH_OUT;
            if (first.in() && !slot.hasItem()) {
                graphics.fakeItem(first.item(), slot.x, slot.y);
                colour = WASH_GHOST;
            }
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, colour);
        }
    }

    /** Sets the tooltip of a hovered locked slot: the item, then a line per lock; false when the slot is free. */
    public static boolean tooltip(GuiGraphicsExtractor graphics, Font font, Slot hovered, MenuLocks locks, int mouseX, int mouseY) {
        if (hovered == null) {
            return false;
        }
        List<SlotLocksPayload.Entry> entries = locks.shownAt(hovered.index);
        if (entries.isEmpty()) {
            return false;
        }
        List<Component> lines = new ArrayList<>();
        if (hovered.hasItem()) {
            lines.add(hovered.getItem().getHoverName());
        }
        for (SlotLocksPayload.Entry entry : entries) {
            lines.add(line(entry));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    /** "Reserved: Shuttle #12 brings 64 Cobblestone", that it waits for a shuttle, or what a pipe still brings. */
    public static Component line(SlotLocksPayload.Entry entry) {
        String direction = entry.in() ? "in" : "out";
        Component item = entry.item().getHoverName();
        if (entry.pipe()) {
            return Component.translatable("gui.servus_shuttles.lock.in_pipe", entry.amount(), item);
        }
        return entry.shuttle()
                .map(shuttle -> Component.translatable("gui.servus_shuttles.lock." + direction, shuttle, entry.amount(), item))
                .orElseGet(() -> Component.translatable("gui.servus_shuttles.lock." + direction + "_waiting", entry.amount(), item));
    }
}
