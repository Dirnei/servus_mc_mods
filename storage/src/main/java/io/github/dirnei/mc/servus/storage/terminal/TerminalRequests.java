package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.access.AccessPointBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity.Request;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity.RequestState;
import io.github.dirnei.mc.servus.storage.terminal.CounterTerminalBlockEntity.Tracked;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Requests of a Counter Terminal on the data line: shipped by the reachable
 * warehouses through their Access Points, booked as shipments end, retried
 * while stock is missing. Server thread only.
 */
public final class TerminalRequests {

    private static final int LAMP_TICKS = 20;
    private static final int RETRY_TICKS = 40;

    private TerminalRequests() {
    }

    /** Called every server tick by the terminal. */
    static void tick(ServerLevel level, CounterTerminalBlockEntity terminal, int ticks) {
        book(level, terminal);
        if (ticks % LAMP_TICKS == 0) {
            terminal.setOnline(!TerminalWarehouses.reachable(level, terminal.getBlockPos()).isEmpty());
        }
        if (ticks % RETRY_TICKS == 0) {
            retry(level, terminal);
        }
    }

    /**
     * Makes a request for {@code amount} of {@code key}.
     *
     * @return an empty string when the request was made, else the refusal key:
     *         {@code offline}, {@code no_access_point}, {@code needs_shuttle}, {@code no_rail} or {@code out_of_stock}
     */
    public static String request(ServerLevel level, CounterTerminalBlockEntity terminal, ItemKey key, int amount) {
        List<WarehouseNetwork> warehouses = TerminalWarehouses.reachable(level, terminal.getBlockPos());
        if (warehouses.isEmpty()) {
            return "offline";
        }
        Request request = terminal.newRequest(key, amount);
        String[] refusal = {""};
        int remaining = ship(terminal, request, warehouses, amount, refusal);
        request.missing = remaining;
        if (remaining > 0) {
            CpuOrders.Placed placed = CpuOrders.create(level, terminal.getBlockPos(), key, remaining, request.id);
            if (placed.made()) {
                request.orderCpu = placed.cpu();
            } else if (request.shipments.isEmpty()) {
                terminal.removeRequest(request.id);
                return refusal[0].isEmpty() ? "out_of_stock" : refusal[0];
            }
        }
        return "";
    }

    /** Ships up to {@code amount} from the warehouses holding the most first; returns what is still missing. */
    private static int ship(CounterTerminalBlockEntity terminal, Request request, List<WarehouseNetwork> warehouses,
                            int amount, String[] refusal) {
        List<WarehouseNetwork> byStock = new ArrayList<>(warehouses);
        byStock.sort(Comparator.comparingLong((WarehouseNetwork warehouse) -> warehouse.available(request.key)).reversed());
        int remaining = amount;
        for (WarehouseNetwork warehouse : byStock) {
            if (remaining <= 0) {
                break;
            }
            long available = warehouse.available(request.key);
            if (available <= 0) {
                continue;
            }
            WarehouseNetwork.Shipping shipping = warehouse.ship(terminal.getBlockPos(), request.key,
                    (int) Math.min(available, remaining), request.id);
            if (shipping.shipped()) {
                AccessPointBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
                request.shipments.add(new Tracked(shipping.accessPoint(), shipment, shipment.planned()));
                remaining -= shipment.planned();
            } else if (refusal[0].isEmpty()) {
                refusal[0] = shipping.refusal();
            }
        }
        return Math.max(0, remaining);
    }

    /** Books ended shipments: what they did not bring is missing again. Drops requests that are done. */
    private static void book(ServerLevel level, CounterTerminalBlockEntity terminal) {
        List<Integer> done = new ArrayList<>();
        for (Request request : terminal.requests()) {
            for (Iterator<Tracked> it = request.shipments.iterator(); it.hasNext(); ) {
                Tracked tracked = it.next();
                if (tracked.shipment().ended()) {
                    request.missing += Math.max(0, tracked.asked() - tracked.shipment().delivered());
                    it.remove();
                }
            }
            if (request.delivered >= request.requested && request.shipments.isEmpty()) {
                done.add(request.id);
            }
        }
        done.forEach(terminal::removeRequest);
    }

    /** Ships the missing part of idle requests again; stock may have arrived meanwhile. */
    private static void retry(ServerLevel level, CounterTerminalBlockEntity terminal) {
        List<WarehouseNetwork> warehouses = null;
        for (Request request : terminal.requests()) {
            if (request.missing <= 0 || !request.shipments.isEmpty()) {
                continue;
            }
            if (warehouses == null) {
                warehouses = TerminalWarehouses.reachable(level, terminal.getBlockPos());
            }
            if (warehouses.isEmpty()) {
                return;
            }
            request.missing = ship(terminal, request, warehouses, request.missing, new String[] {""});
        }
    }

    /** Cancels a request: queued picks and its work order go, shipments end and their items go back into storage. */
    public static void cancel(ServerLevel level, CounterTerminalBlockEntity terminal, int requestId) {
        Request request = terminal.request(requestId);
        if (request == null) {
            return;
        }
        for (WarehouseNetwork warehouse : TerminalWarehouses.reachable(level, terminal.getBlockPos())) {
            warehouse.dropQueuedFor(requestId);
        }
        CpuOrders.cancelForRequest(level, terminal.getBlockPos(), requestId);
        for (Tracked tracked : request.shipments) {
            if (level.getBlockEntity(tracked.accessPoint()) instanceof AccessPointBlockEntity point) {
                point.cancel(tracked.shipment());
            }
        }
        terminal.removeRequest(requestId);
    }

    /** The open requests as the GUI lists them. */
    public static List<RequestRow> rows(ServerLevel level, CounterTerminalBlockEntity terminal, List<WarehouseNetwork> warehouses) {
        List<RequestRow> rows = new ArrayList<>();
        for (Request request : terminal.requests()) {
            rows.add(new RequestRow(request.id, request.key.stack(1), request.requested, request.delivered, request.missing,
                    state(level, terminal, request, warehouses).name()));
        }
        return rows;
    }

    static RequestState state(ServerLevel level, CounterTerminalBlockEntity terminal, Request request, List<WarehouseNetwork> warehouses) {
        for (Tracked tracked : request.shipments) {
            if (level.getBlockEntity(tracked.accessPoint()) instanceof AccessPointBlockEntity point && point.isSending(tracked.shipment())) {
                // Packets that find no room wait at the terminal.
                boolean fits = ItemHandlerHelper.insertItemStacked(terminal.output(), request.key.stack(1), true).isEmpty();
                return fits ? RequestState.SENDING : RequestState.WAITING_ROOM;
            }
        }
        if (!request.shipments.isEmpty()) {
            return RequestState.PICKING;
        }
        if (CpuOrders.cpuAt(level, request.orderCpu).filter(cpu -> cpu.book().hasOpenOrderFor(request.id)).isPresent()) {
            return RequestState.PRODUCING;
        }
        return RequestState.MISSING;
    }

    /** The first reachable warehouse, for rules and pushes. */
    public static Optional<WarehouseNetwork> first(ServerLevel level, BlockPos pos) {
        List<WarehouseNetwork> warehouses = TerminalWarehouses.reachable(level, pos);
        return warehouses.isEmpty() ? Optional.empty() : Optional.of(warehouses.get(0));
    }
}
