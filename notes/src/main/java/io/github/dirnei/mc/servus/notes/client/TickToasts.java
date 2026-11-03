package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.config.NotesClientConfig;
import io.github.dirnei.mc.servus.notes.model.NewTicks;
import io.github.dirnei.mc.servus.notes.model.NoteList;
import io.github.dirnei.mc.servus.notes.model.Scope;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

/** Vanilla toasts for tasks another player ticked on a list this player sees, never for Server lists. */
final class TickToasts {

    private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId();
    private static final int MAX_TOASTS = 3;

    private TickToasts() {
    }

    /** Shows a toast for each task ticked by someone else between the two snapshots, at most three per snapshot. */
    static void show(NoteList before, NoteList after) {
        if (after.scope() == Scope.SERVER || !NotesClientConfig.tickToasts()) {
            return;
        }
        List<NewTicks.Tick> ticks = NewTicks.between(before, after, ClientNotes.self());
        if (ticks.isEmpty()) {
            return;
        }
        Component title = Component.literal(after.name());
        int single = ticks.size() <= MAX_TOASTS ? ticks.size() : MAX_TOASTS - 1;
        for (NewTicks.Tick tick : ticks.subList(0, single)) {
            add(title, Component.translatable("toast.servus_notes.ticked", tick.by().name(), tick.task().text()));
        }
        if (single < ticks.size()) {
            add(title, Component.translatable("toast.servus_notes.ticked_more", ticks.size() - single));
        }
    }

    private static void add(Component title, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getToastManager().addToast(SystemToast.multiline(minecraft, ID, title, message));
    }
}
