package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.production.ProductionPayload;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.DuctRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.JobRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.MachineRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PackingRow;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.PatternsView;
import io.github.dirnei.mc.servus.shuttles.production.ProductionRows.StorageRow;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

/**
 * The last CPUs, jobs, machines and patterns the server sent, kept per
 * Production Terminal for the session, so a terminal opened again shows its
 * last content at once while the fresh one is on its way. Client main thread only.
 */
public final class ClientProduction {

    private static final TerminalStore<GlobalPos, ProductionPayload> STORE = new TerminalStore<>();

    private static ProductionPayload shown;

    private ClientProduction() {
    }

    /** A Production Terminal screen opens: show what was last seen of it, if anything. */
    public static void open(BlockPos terminal) {
        STORE.open(key(terminal));
        shown = STORE.shown();
    }

    /** The screen closed; its content stays cached. */
    public static void close() {
        STORE.close();
        shown = null;
    }

    /** Forget every terminal, when leaving the world. */
    public static void forgetAll() {
        STORE.forgetAll();
        shown = null;
    }

    public static void update(ProductionPayload payload) {
        STORE.update(key(payload.terminal()), payload);
        shown = STORE.shown();
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

    public static List<JobRow> jobs() {
        return shown == null ? List.of() : shown.jobs();
    }

    public static List<MachineRow> machines() {
        return shown == null ? List.of() : shown.machines();
    }

    public static List<PackingRow> packing() {
        return shown == null ? List.of() : shown.packing();
    }

    public static List<DuctRow> busiest() {
        return shown == null ? List.of() : shown.busiest();
    }

    public static List<StorageRow> storage() {
        return shown == null ? List.of() : shown.storage();
    }

    /** Whether the shown line reaches two or more warehouses. */
    public static boolean lineConflict() {
        return shown != null && shown.lineConflict();
    }

    public static PatternsView patterns() {
        return shown == null ? PatternsView.EMPTY : shown.patterns();
    }

    public static int version() {
        return STORE.version();
    }
}
