package io.github.dirnei.mc.servus.shuttles.client;

import java.util.HashMap;
import java.util.Map;

/**
 * The content last received per terminal and which terminal a screen shows.
 * A screen calls {@link #open} every time it is shown (also when it returns
 * after another screen was in front of it) and {@link #close} when it goes
 * away; content keeps arriving and is cached either way. Pure, no Minecraft
 * types.
 *
 * @param <K> terminal key
 * @param <P> content
 */
public final class TerminalStore<K, P> {

    private final Map<K, P> cache = new HashMap<>();
    private K open;
    private P shown;
    private int version;

    /**
     * Shows the content last received for the terminal, if any.
     *
     * @return true when a different terminal than before is shown now
     */
    public boolean open(K terminal) {
        boolean changed = terminal == null || !terminal.equals(open);
        open = terminal;
        shown = terminal == null ? null : cache.get(terminal);
        version++;
        return changed;
    }

    /** No screen shows a terminal any more; the content stays cached. */
    public void close() {
        open = null;
        shown = null;
        version++;
    }

    /** Forgets every terminal. */
    public void forgetAll() {
        cache.clear();
        close();
    }

    /** Content for a terminal arrived: cache it, and show it when that terminal is open. */
    public void update(K terminal, P content) {
        if (terminal == null) {
            return;
        }
        cache.put(terminal, content);
        if (terminal.equals(open)) {
            shown = content;
            version++;
        }
    }

    /** The content of the open terminal, or null while none is known. */
    public P shown() {
        return shown;
    }

    /** Bumps whenever the shown content changes. */
    public int version() {
        return version;
    }
}
