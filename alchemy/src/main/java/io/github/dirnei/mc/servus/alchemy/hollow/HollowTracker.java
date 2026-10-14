package io.github.dirnei.mc.servus.alchemy.hollow;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The Gnome Hollows loaded on the client, so the reveal renderer can draw them without scanning blocks.
 * Only client block entities add themselves; the server never touches it.
 */
public final class HollowTracker {

    private static final Set<GnomeHollowBlockEntity> HOLLOWS = Collections.synchronizedSet(new LinkedHashSet<>());

    private HollowTracker() {
    }

    public static void add(GnomeHollowBlockEntity hollow) {
        HOLLOWS.add(hollow);
    }

    public static void remove(GnomeHollowBlockEntity hollow) {
        HOLLOWS.remove(hollow);
    }

    /** A copy of the tracked Hollows, safe to iterate while chunks load. */
    public static Set<GnomeHollowBlockEntity> hollows() {
        synchronized (HOLLOWS) {
            return new LinkedHashSet<>(HOLLOWS);
        }
    }
}
