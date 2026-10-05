package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import java.util.List;

/** The last stock the server sent for the open terminal. */
public final class ClientStock {

    private static volatile boolean online;
    private static volatile String reason = "";
    private static volatile List<StockEntry> entries = List.of();
    private static volatile int version;

    private ClientStock() {
    }

    public static void update(StockSyncPayload payload) {
        online = payload.online();
        reason = payload.reason();
        entries = List.copyOf(payload.entries());
        version++;
    }

    public static void clear() {
        online = false;
        reason = "";
        entries = List.of();
        version++;
    }

    public static boolean online() {
        return online;
    }

    public static String reason() {
        return reason;
    }

    public static List<StockEntry> entries() {
        return entries;
    }

    /** Bumps on every update so screens can refilter lazily. */
    public static int version() {
        return version;
    }
}
