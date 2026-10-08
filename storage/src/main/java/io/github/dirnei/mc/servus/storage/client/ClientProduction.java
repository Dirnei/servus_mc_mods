package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.PatternsView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

/**
 * The last CPUs, orders, producers and patterns the server sent, kept per
 * Production Terminal for the session, so a terminal opened again shows its
 * last content at once while the fresh one is on its way. Client main thread only.
 */
public final class ClientProduction {

    private static final Map<GlobalPos, ProductionPayload> CACHE = new HashMap<>();

    private static GlobalPos open;
    private static ProductionPayload shown;
    private static int version;

    private ClientProduction() {
    }

    /** A Production Terminal screen opens: show what was last seen of it, if anything. */
    public static void open(BlockPos terminal) {
        open = key(terminal);
        shown = open == null ? null : CACHE.get(open);
        version++;
    }

    /** The screen closed; its content stays cached. */
    public static void close() {
        open = null;
        shown = null;
        version++;
    }

    /** Forget every terminal, when leaving the world. */
    public static void forgetAll() {
        CACHE.clear();
        close();
    }

    public static void update(ProductionPayload payload) {
        GlobalPos key = key(payload.terminal());
        if (key == null) {
            return;
        }
        CACHE.put(key, payload);
        if (key.equals(open)) {
            shown = payload;
            version++;
        }
    }

    private static GlobalPos key(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? null : GlobalPos.of(minecraft.level.dimension(), pos);
    }

    /** Whether content for the open terminal exists, fresh or from earlier in the session. */
    public static boolean loaded() {
        return shown != null;
    }

    public static boolean online() {
        return shown != null && shown.online();
    }

    public static String reason() {
        return shown == null ? "" : shown.reason();
    }

    public static List<CpuRow> cpus() {
        return shown == null ? List.of() : shown.cpus();
    }

    public static List<MachineRow> machines() {
        return shown == null ? List.of() : shown.machines();
    }

    public static PatternsView patterns() {
        return shown == null ? PatternsView.EMPTY : shown.patterns();
    }

    public static int version() {
        return version;
    }
}
