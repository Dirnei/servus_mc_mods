package io.github.dirnei.mc.servus.shuttles.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans the whole slots a Packing Station reserves for an offer into To
 * storage: as many free slots as the amount needs at the item's stack size,
 * as far as there are free slots, each holding up to a stack, and the one
 * slot an Inbound Station reserves for an Item Pipe. Pure Java.
 */
public final class SlotGrant {

    /** One reserved slot and how many items it is reserved for. */
    public record Part(int slot, int count) {
    }

    private SlotGrant() {
    }

    /** The slots for {@code amount} items of stack size {@code stackSize}, taken from {@code freeSlots} in order. */
    public static List<Part> plan(List<Integer> freeSlots, int stackSize, int amount) {
        List<Part> parts = new ArrayList<>();
        int left = amount;
        int perSlot = Math.max(1, stackSize);
        for (int slot : freeSlots) {
            if (left <= 0) {
                break;
            }
            int count = Math.min(perSlot, left);
            parts.add(new Part(slot, count));
            left -= count;
        }
        return parts;
    }

    /**
     * The one slot an Inbound Station reserves for a pipe's offer: the first
     * free slot for the offered amount, at most a stack and at most
     * {@code room}; empty without a free slot or room.
     */
    public static List<Part> one(List<Integer> freeSlots, int stackSize, int amount, int room) {
        int count = Math.min(Math.min(amount, Math.max(1, stackSize)), room);
        if (freeSlots.isEmpty() || count <= 0) {
            return List.of();
        }
        return List.of(new Part(freeSlots.get(0), count));
    }

    /** How many items the parts hold together. */
    public static int total(List<Part> parts) {
        int sum = 0;
        for (Part part : parts) {
            sum += part.count();
        }
        return sum;
    }
}
