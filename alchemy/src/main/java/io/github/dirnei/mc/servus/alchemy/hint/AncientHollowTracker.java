package io.github.dirnei.mc.servus.alchemy.hint;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Ancient Hollows loaded on the client, so the reveal renderer can draw the old gnome without scanning blocks.
 * Only client block entities add themselves; the server never touches it.
 */
public final class AncientHollowTracker {

    private static final Set<AncientHollowBlockEntity> HOLLOWS = Collections.synchronizedSet(new LinkedHashSet<>());

    private AncientHollowTracker() {
    }

    public static void add(AncientHollowBlockEntity hollow) {
        HOLLOWS.add(hollow);
    }

    public static void remove(AncientHollowBlockEntity hollow) {
        HOLLOWS.remove(hollow);
    }

    /** A copy of the tracked Hollows, safe to iterate while chunks load. */
    public static Set<AncientHollowBlockEntity> hollows() {
        synchronized (HOLLOWS) {
            return new LinkedHashSet<>(HOLLOWS);
        }
    }
}
