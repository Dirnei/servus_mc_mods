package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import java.util.List;

/** The last stock, requests and message the server sent for the open terminal. */
public final class ClientStock {

    private static volatile boolean online;
    private static volatile String reason = "";
    private static volatile List<StockEntry> entries = List.of();
    private static volatile List<RequestRow> requests = List.of();
    private static volatile String message = "";
    private static volatile int version;

    private ClientStock() {
    }

    public static void update(StockSyncPayload payload) {
        online = payload.online();
        reason = payload.reason();
        entries = List.copyOf(payload.entries());
        requests = List.copyOf(payload.requests());
        message = payload.message();
        version++;
    }

    public static void clear() {
        online = false;
        reason = "";
        entries = List.of();
        requests = List.of();
        message = "";
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

    public static List<RequestRow> requests() {
        return requests;
    }

    public static String message() {
        return message;
    }

    /** Bumps on every update so screens can refilter lazily. */
    public static int version() {
        return version;
    }
}
