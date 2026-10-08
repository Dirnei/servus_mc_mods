package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.production.ProductionPayload;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.CpuRow;
import io.github.dirnei.mc.servus.storage.production.ProductionRows.MachineRow;
import java.util.List;

/** The last CPUs, orders and producers the server sent for the open Production Terminal. */
public final class ClientProduction {

    private static volatile boolean online;
    private static volatile String reason = "";
    private static volatile List<CpuRow> cpus = List.of();
    private static volatile List<MachineRow> machines = List.of();
    private static volatile int version;

    private ClientProduction() {
    }

    public static void update(ProductionPayload payload) {
        online = payload.online();
        reason = payload.reason();
        cpus = List.copyOf(payload.cpus());
        machines = List.copyOf(payload.machines());
        version++;
    }

    public static void clear() {
        online = false;
        reason = "";
        cpus = List.of();
        machines = List.of();
        version++;
    }

    public static boolean online() {
        return online;
    }

    public static String reason() {
        return reason;
    }

    public static List<CpuRow> cpus() {
        return cpus;
    }

    public static List<MachineRow> machines() {
        return machines;
    }

    public static int version() {
        return version;
    }
}
