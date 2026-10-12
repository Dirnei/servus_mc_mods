package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.storage.terminal.RequestRow;
import io.github.dirnei.mc.servus.storage.terminal.RuleRow;
import io.github.dirnei.mc.servus.storage.terminal.StockEntry;
import io.github.dirnei.mc.servus.storage.terminal.StockSyncPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

/**
 * The last stock, craftables, requests, message and craft preview the server
 * sent, kept per Storage Terminal for the session, so a terminal opened again
 * shows its last content at once while the fresh one is on its way.
 * Client main thread only.
 */
public final class ClientStock {

    private static final TerminalStore<GlobalPos, StockSyncPayload> STORE = new TerminalStore<>();

    private static StockSyncPayload shown;
    private static CraftPreviewPayload preview;

    private ClientStock() {
    }

    /** A Storage Terminal screen opens: show what was last seen of it, if anything. */
    public static void open(BlockPos terminal) {
        if (STORE.open(key(terminal))) {
            preview = null;
        }
        shown = STORE.shown();
    }

    /** The screen closed; its content stays cached. */
    public static void close() {
        STORE.close();
        shown = null;
        preview = null;
    }

    /** Forget every terminal, when leaving the world. */
    public static void forgetAll() {
        STORE.forgetAll();
        shown = null;
        preview = null;
    }

    public static void update(StockSyncPayload payload) {
        STORE.update(key(payload.terminal()), payload);
        shown = STORE.shown();
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof io.github.dirnei.mc.servus.storage.menu.StorageTerminalMenu menu
                && menu.terminal().getBlockPos().equals(payload.terminal())) {
            menu.clientView(payload.view());
        }
    }

    private static GlobalPos key(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : GlobalPos.of(minecraft.level.dimension(), pos);
    }

    public static void preview(CraftPreviewPayload payload) {
        preview = payload;
    }

    /** Whether content for the open terminal exists, fresh or from earlier in the session. */
    public static boolean loaded() {
        return shown != null;
    }

    public static boolean online() {
        return shown != null && shown.online();
    }

    /** Whether a warehouse the open terminal reaches has no power. */
    public static boolean noPower() {
        return shown != null && shown.noPower();
    }

    public static String reason() {
        return shown == null ? "" : shown.reason();
    }

    public static List<StockEntry> entries() {
        return shown == null ? List.of() : shown.entries();
    }

    /** The other sites of the team the terminal can show. */
    public static List<io.github.dirnei.mc.servus.storage.terminal.SiteRow> sites() {
        return shown == null ? List.of() : shown.sites();
    }

    /** The key of the site the grid shows, empty for this site. */
    public static String view() {
        return shown == null ? "" : shown.view();
    }

    /** The state of the site shown, null for this site. */
    public static io.github.dirnei.mc.servus.storage.site.RemoteSite.State viewState() {
        if (shown == null || shown.viewState() < 0) {
            return null;
        }
        io.github.dirnei.mc.servus.storage.site.RemoteSite.State[] states = io.github.dirnei.mc.servus.storage.site.RemoteSite.State.values();
        return shown.viewState() < states.length ? states[shown.viewState()] : null;
    }

    /** The name of the site shown, empty for this site. */
    public static String viewName() {
        String view = view();
        return sites().stream().filter(row -> row.key().equals(view)).map(io.github.dirnei.mc.servus.storage.terminal.SiteRow::name)
                .findFirst().orElse("");
    }

    public static List<RequestRow> requests() {
        return shown == null ? List.of() : shown.requests();
    }

    public static String message() {
        return shown == null ? "" : shown.message();
    }

    public static List<RuleRow> rules() {
        return shown == null ? List.of() : shown.rules();
    }

    public static List<ItemStack> craftables() {
        return shown == null ? List.of() : shown.craftables();
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
        for (ItemStack craftable : craftables()) {
            if (ItemStack.isSameItemSameComponents(craftable, stack)) {
                return true;
            }
        }
        return false;
    }

    /** The rule for a stack, or null. */
    public static RuleRow ruleFor(ItemStack stack) {
        for (RuleRow rule : rules()) {
            if (ItemStack.isSameItemSameComponents(rule.stack(), stack)) {
                return rule;
            }
        }
        return null;
    }

    /** Bumps whenever the shown content changes so screens can refilter lazily. */
    public static int version() {
        return STORE.version();
    }
}
