package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.pattern.AnyGroup;
import io.github.dirnei.mc.servus.shuttles.terminal.MaterialCraftable;
import io.github.dirnei.mc.servus.shuttles.menu.StorageTerminalMenu;
import io.github.dirnei.mc.servus.shuttles.site.RemoteSite;
import io.github.dirnei.mc.servus.shuttles.terminal.CraftPreviewPayload;
import io.github.dirnei.mc.servus.shuttles.terminal.RequestRow;
import io.github.dirnei.mc.servus.shuttles.terminal.RuleRow;
import io.github.dirnei.mc.servus.shuttles.terminal.SiteRow;
import io.github.dirnei.mc.servus.shuttles.terminal.StockEntry;
import io.github.dirnei.mc.servus.shuttles.terminal.StockSyncPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof StorageTerminalMenu menu
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

    /** Whether the open terminal's data line has a formed Crafting CPU and a Crafting Station for the order grid. */
    public static boolean orderGrid() {
        return shown != null && shown.orderGrid();
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
    public static List<SiteRow> sites() {
        return shown == null ? List.of() : shown.sites();
    }

    /** The key of the site the grid shows, empty for this site. */
    public static String view() {
        return shown == null ? "" : shown.view();
    }

    /** The state of the site shown, null for this site. */
    public static RemoteSite.State viewState() {
        if (shown == null || shown.viewState() < 0) {
            return null;
        }
        RemoteSite.State[] states = RemoteSite.State.values();
        return shown.viewState() < states.length ? states[shown.viewState()] : null;
    }

    /** The name of the site shown, empty for this site. */
    public static String viewName() {
        String view = view();
        return sites().stream().filter(row -> row.key().equals(view)).map(SiteRow::name)
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

    /** What the material of a material pattern's craftable entry follows, or null for any other stack. */
    public static AnyGroup follows(ItemStack stack) {
        if (shown != null) {
            for (MaterialCraftable material : shown.materials()) {
                if (ItemStack.isSameItemSameComponents(material.generic(), stack)) {
                    return material.follows();
                }
            }
        }
        return null;
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
