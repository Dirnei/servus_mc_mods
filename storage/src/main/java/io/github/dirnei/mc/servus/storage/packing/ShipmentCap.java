package io.github.dirnei.mc.servus.storage.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * After a Packing Station loads, its shipments may only wait for what its
 * outgoing buffer holds: picks that were on their way may never come. Shares
 * the buffer among the shipments of one item, oldest first. Pure Java.
 */
public final class ShipmentCap {

    private ShipmentCap() {
    }

    /**
     * @param missing  what each shipment still waits for, oldest first
     * @param buffered how many of the item sit in the outgoing buffer
     * @return per shipment, how many it no longer waits for
     */
    public static List<Integer> shortfalls(List<Integer> missing, int buffered) {
        List<Integer> cuts = new ArrayList<>(missing.size());
        int left = Math.max(0, buffered);
        for (int want : missing) {
            int kept = Math.min(Math.max(0, want), left);
            left -= kept;
            cuts.add(Math.max(0, want) - kept);
        }
        return cuts;
    }

    /**
     * How many items a relay, a data line shipment that passes on a wireless
     * transfer, no longer waits for: it brings at most what the transfer still
     * asks while it runs, and what it delivered once it ended.
     *
     * @param relayAsked    what the relay asks now
     * @param feedAsked     what the wireless transfer asks now
     * @param feedDelivered what the wireless transfer delivered
     * @param feedEnded     whether the wireless transfer ended
     */
    public static int relayCut(int relayAsked, int feedAsked, int feedDelivered, boolean feedEnded) {
        int cap = Math.max(0, feedEnded ? feedDelivered : feedAsked);
        return Math.max(0, relayAsked - cap);
    }
}
