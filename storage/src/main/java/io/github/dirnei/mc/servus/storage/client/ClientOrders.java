package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.board.MachineRow;
import io.github.dirnei.mc.servus.storage.board.OrderBoardPayload;
import io.github.dirnei.mc.servus.storage.board.OrderRow;
import java.util.List;

/** The last machines and orders the server sent for the open order board. */
public final class ClientOrders {

    private static volatile boolean online;
    private static volatile String reason = "";
    private static volatile List<MachineRow> machines = List.of();
    private static volatile List<OrderRow> orders = List.of();
    private static volatile int version;

    private ClientOrders() {
    }

    public static void update(OrderBoardPayload payload) {
        online = payload.online();
        reason = payload.reason();
        machines = List.copyOf(payload.machines());
        orders = List.copyOf(payload.orders());
        version++;
    }

    public static void clear() {
        online = false;
        reason = "";
        machines = List.of();
        orders = List.of();
        version++;
    }

    public static boolean online() {
        return online;
    }

    public static String reason() {
        return reason;
    }

    public static List<MachineRow> machines() {
        return machines;
    }

    public static List<OrderRow> orders() {
        return orders;
    }

    public static int version() {
        return version;
    }
}
