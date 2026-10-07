package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
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
    private static volatile List<RuleRow> rules = List.of();
    private static volatile int version;

    private ClientStock() {
    }

    public static void update(StockSyncPayload payload) {
        online = payload.online();
        reason = payload.reason();
        entries = List.copyOf(payload.entries());
        requests = List.copyOf(payload.requests());
        message = payload.message();
        rules = List.copyOf(payload.rules());
        version++;
    }

    public static void clear() {
        online = false;
        reason = "";
        entries = List.of();
        requests = List.of();
        message = "";
        rules = List.of();
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

    public static List<RuleRow> rules() {
        return rules;
    }

    /** The rule for a stack, or null. */
    public static RuleRow ruleFor(net.minecraft.world.item.ItemStack stack) {
        for (RuleRow rule : rules) {
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(rule.stack(), stack)) {
                return rule;
            }
        }
        return null;
    }

    /** Bumps on every update so screens can refilter lazily. */
    public static int version() {
        return version;
    }
}
