package io.github.dirnei.mc.servus.shuttles.terminal;

import java.util.List;

/**
 * How much of an offer a Storage Terminal grants a request: the room left in
 * the request's slots and the items it still waits for, both less what grants
 * it already gave reserve. Pure, no Minecraft imports.
 */
public final class RequestRoom {

    /** The count of a slot that holds another item and so has no room for this one. */
    public static final int OTHER_ITEM = -1;

    private RequestRoom() {
    }

    /**
     * The grant for an offer of {@code amount} items.
     *
     * @param need      how many the request still waits for
     * @param counts    per slot of the request: how many of the item it holds, or {@link #OTHER_ITEM}
     * @param stackSize how many of the item one slot holds
     * @param held      how many open grants of the request already reserve
     */
    public static int grant(int amount, int need, List<Integer> counts, int stackSize, int held) {
        int room = 0;
        for (int count : counts) {
            if (count != OTHER_ITEM) {
                room += Math.max(0, stackSize - count);
            }
        }
        return Math.max(0, Math.min(amount, Math.min(room - held, need - held)));
    }
}
