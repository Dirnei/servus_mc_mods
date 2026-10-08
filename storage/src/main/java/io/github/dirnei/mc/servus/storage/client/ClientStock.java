package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** The last stock, craftables, requests, message and craft preview the server sent for the open terminal. */
public final class ClientStock {

    private static volatile boolean loaded;
    private static volatile boolean online;
    private static volatile String reason = "";
    private static volatile List<StockEntry> entries = List.of();
    private static volatile List<RequestRow> requests = List.of();
    private static volatile String message = "";
    private static volatile List<RuleRow> rules = List.of();
    private static volatile List<ItemStack> craftables = List.of();
    private static volatile CraftPreviewPayload preview;
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
        craftables = List.copyOf(payload.craftables());
        loaded = true;
        version++;
    }

    public static void preview(CraftPreviewPayload payload) {
        preview = payload;
    }

    public static void clear() {
        loaded = false;
        online = false;
        reason = "";
        entries = List.of();
        requests = List.of();
        message = "";
        rules = List.of();
        craftables = List.of();
        preview = null;
        version++;
    }

    /** Whether the server sent the content since the screen opened. */
    public static boolean loaded() {
        return loaded;
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

    public static List<ItemStack> craftables() {
        return craftables;
    }

    /** The last craft preview, or null. */
    public static CraftPreviewPayload preview() {
        return preview;
    }

    public static void clearPreview() {
        preview = null;
    }

    /** Whether a producer of the line can make the item. */
    public static boolean craftable(ItemStack stack) {
        for (ItemStack craftable : craftables) {
            if (ItemStack.isSameItemSameComponents(craftable, stack)) {
                return true;
            }
        }
        return false;
    }

    /** The rule for a stack, or null. */
    public static RuleRow ruleFor(ItemStack stack) {
        for (RuleRow rule : rules) {
            if (ItemStack.isSameItemSameComponents(rule.stack(), stack)) {
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
