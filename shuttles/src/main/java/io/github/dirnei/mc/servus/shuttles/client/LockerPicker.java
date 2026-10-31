package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.handheld.HandheldLockersPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * The state of the Handheld's locker picker: waiting for the server, then
 * the lockers sorted by distance from the player, nearest first, with the
 * nearest one that takes the order selected. Lockers that cannot take it
 * cannot be selected.
 */
final class LockerPicker {

    /** One locker of the list with its distance from the player in whole blocks. */
    record Entry(HandheldLockersPayload.Row row, int distance) {
    }

    private boolean waiting = true;
    private String problem = "";
    private List<Entry> entries = List.of();
    private int selected = -1;

    /** Takes the server's answer; distances are measured from {@code player} to each dock. */
    void accept(HandheldLockersPayload payload, Vec3 player) {
        waiting = false;
        problem = payload.problem();
        List<Entry> list = new ArrayList<>();
        for (HandheldLockersPayload.Row row : payload.lockers()) {
            list.add(new Entry(row, (int) Math.round(Vec3.atCenterOf(row.dock()).distanceTo(player))));
        }
        list.sort(Comparator.comparingInt(Entry::distance));
        entries = List.copyOf(list);
        selected = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).row().fits()) {
                selected = i;
                break;
            }
        }
    }

    boolean waiting() {
        return waiting;
    }

    List<Entry> entries() {
        return entries;
    }

    int selected() {
        return selected;
    }

    /** Selects the locker at that place of the list when it takes the order. */
    void select(int index) {
        if (index >= 0 && index < entries.size() && entries.get(index).row().fits()) {
            selected = index;
        }
    }

    /** The dock of the selected locker, empty while ORDER cannot be used. */
    Optional<BlockPos> chosen() {
        return reason().isEmpty() ? Optional.of(entries.get(selected).row().dock()) : Optional.empty();
    }

    /** The lang key suffix of why ORDER cannot be used, empty when it can. */
    String reason() {
        if (waiting) {
            return "picker.waiting";
        }
        if (!problem.isEmpty()) {
            return "picker." + problem;
        }
        return selected < 0 ? "picker.none_fits" : "";
    }
}
