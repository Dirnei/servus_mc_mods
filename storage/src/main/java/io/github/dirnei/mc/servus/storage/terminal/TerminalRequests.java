package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.cpu.CpuOrders;
import io.github.dirnei.mc.servus.storage.order.OrderBook;
import io.github.dirnei.mc.servus.storage.order.ProductionLedger;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity.Request;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity.RequestState;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity.Tracked;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Requests of a Storage Terminal on the data line: shipped by the reachable
 * warehouses through their Packing Stations, booked as shipments end, retried
 * while stock is missing. Server thread only.
 */
public final class TerminalRequests {

    private static final int LAMP_TICKS = 20;
    private static final int RETRY_TICKS = 40;

    private TerminalRequests() {
    }

    /** Called every server tick by the terminal. */
    static void tick(ServerLevel level, StorageTerminalBlockEntity terminal, int ticks) {
        terminal.seeIds(level);
        book(level, terminal);
        if (ticks % LAMP_TICKS == 0) {
            terminal.setOnline(!TerminalWarehouses.reachable(level, terminal.getBlockPos()).isEmpty());
        }
        if (ticks % RETRY_TICKS == 0) {
            retry(level, terminal);
            if (!terminal.requests().isEmpty()) {
                terminal.setChanged();
            }
        }
    }

    /**
     * Makes a request for {@code amount} of {@code key}.
     *
     * @return an empty string when the request was made, else the refusal key:
     *         {@code offline}, {@code no_packing_station}, {@code needs_shuttle}, {@code no_rail} or {@code out_of_stock}
     */
    public static String request(ServerLevel level, StorageTerminalBlockEntity terminal, ItemKey key, int amount) {
        List<WarehouseNetwork> warehouses = TerminalWarehouses.reachable(level, terminal.getBlockPos());
        if (warehouses.isEmpty()) {
            return "offline";
        }
        Request request = terminal.newRequest(level, key, amount);
        String[] refusal = {""};
        int remaining = ship(terminal, request, warehouses, amount, refusal);
        request.missing = remaining;
        if (remaining > 0) {
            CpuOrders.Placed placed = CpuOrders.create(level, terminal.getBlockPos(), key, remaining, request.id);
            if (placed.made()) {
                request.orderCpu = placed.cpu();
                placed.created().order().deliverTo(terminal.getBlockPos());
            } else if (request.shipments.isEmpty()) {
                terminal.removeRequest(request.id);
                return refusal[0].isEmpty() ? "out_of_stock" : refusal[0];
            }
        }
        return "";
    }

    /**
     * Crafts {@code amount} of {@code key} on a CPU of the line, independent of
     * the item's own stock, for a new request whose results land in the output slots.
     *
     * @return an empty string when the order was made, else the refusal key:
     *         {@code offline}, {@code no_cpu}, {@code no_producer} or {@code missing}
     */
    public static String craft(ServerLevel level, StorageTerminalBlockEntity terminal, ItemKey key, int amount) {
        if (TerminalWarehouses.reachable(level, terminal.getBlockPos()).isEmpty()) {
            return "offline";
        }
        OrderBook.Preview preview =
                TerminalCraftables.preview(level, terminal.getBlockPos(), key, amount);
        if (!preview.refusal().isEmpty()) {
            return preview.refusal();
        }
        if (!preview.missing().isEmpty()) {
            return "missing";
        }
        Request request = terminal.newRequest(level, key, amount);
        request.missing = amount;
        CpuOrders.Placed placed = CpuOrders.create(level, terminal.getBlockPos(), key, amount, request.id);
        if (!placed.made()) {
            terminal.removeRequest(request.id);
            return placed.refusal();
        }
        request.orderCpu = placed.cpu();
        placed.created().order().deliverTo(terminal.getBlockPos());
        return "";
    }

    /** Ships up to {@code amount} from the warehouses holding the most first; returns what is still missing. */
    private static int ship(StorageTerminalBlockEntity terminal, Request request, List<WarehouseNetwork> warehouses,
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
                PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
                request.shipments.add(new Tracked(shipping.packingStation(), shipment.id(), shipment.planned()));
                terminal.setChanged();
                remaining -= shipment.planned();
            } else if (refusal[0].isEmpty()) {
                refusal[0] = shipping.refusal();
            }
        }
        return Math.max(0, remaining);
    }

    /** Books ended shipments: what they did not bring is missing again. Drops requests that are done. */
    private static void book(ServerLevel level, StorageTerminalBlockEntity terminal) {
        List<Integer> done = new ArrayList<>();
        ProductionLedger ledger = ProductionLedger.get(level);
        for (Request request : terminal.requests()) {
            for (Iterator<Tracked> it = request.shipments.iterator(); it.hasNext(); ) {
                Tracked tracked = it.next();
                ProductionLedger.Transfer transfer = ledger.transfer(tracked.shipmentId());
                // A shipment the ledger forgot is shipped again rather than waited for.
                if (transfer == null || transfer.ended()) {
                    int delivered = transfer == null ? 0 : transfer.delivered();
                    request.missing += Math.max(0, tracked.asked() - delivered);
                    if (request.remote != null) {
                        // A transfer's items arrive in this site's storage, not in the output slots.
                        request.delivered += delivered;
                    }
                    it.remove();
                    terminal.setChanged();
                }
            }
            if (request.delivered >= request.requested && request.shipments.isEmpty()
                    || request.remote != null && request.shipped && request.shipments.isEmpty()) {
                done.add(request.id);
            }
        }
        done.forEach(terminal::removeRequest);
    }

    /** Whether a work order for the request is still open; its results come straight to the terminal. */
    private static boolean producing(ServerLevel level, Request request) {
        return CpuOrders.cpuAt(level, request.orderCpu).filter(cpu -> cpu.book().hasOpenOrderFor(request.id)).isPresent();
    }

    /** How many of the request's item may still be sent straight to the terminal: what it misses. */
    public static int need(StorageTerminalBlockEntity terminal, int requestId) {
        Request request = terminal.request(requestId);
        return request == null ? 0 : Math.max(0, request.missing);
    }

    /** Items are on their way straight from a producer: they no longer count as missing. */
    public static void expectDirect(StorageTerminalBlockEntity terminal, int requestId, int count) {
        Request request = terminal.request(requestId);
        if (request != null) {
            request.missing = Math.max(0, request.missing - count);
            terminal.setChanged();
        }
    }

    /** Items sent straight from a producer did not all arrive: the rest is missing again. */
    public static void directShort(StorageTerminalBlockEntity terminal, int requestId, int count) {
        Request request = terminal.request(requestId);
        if (request != null && count > 0) {
            request.missing += count;
            terminal.setChanged();
        }
    }

    /** Ships the missing part of idle requests again; stock may have arrived meanwhile. */
    private static void retry(ServerLevel level, StorageTerminalBlockEntity terminal) {
        List<WarehouseNetwork> warehouses = null;
        for (Request request : terminal.requests()) {
            if (request.remote != null) {
                // A transfer waits for its site to come online, then is shipped once.
                if (!request.shipped) {
                    RemoteTransfers.ship(level, terminal, request);
                }
                continue;
            }
            if (request.missing <= 0 || !request.shipments.isEmpty() || producing(level, request)) {
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
    public static void cancel(ServerLevel level, StorageTerminalBlockEntity terminal, int requestId) {
        Request request = terminal.request(requestId);
        if (request == null) {
            return;
        }
        for (WarehouseNetwork warehouse : TerminalWarehouses.reachable(level, terminal.getBlockPos())) {
            warehouse.dropQueuedFor(requestId);
        }
        if (request.remote != null) {
            RemoteTransfers.dropQueued(level, request);
        }
        CpuOrders.cancelForRequest(level, terminal.getBlockPos(), requestId);
        for (Tracked tracked : request.shipments) {
            if (level.isLoaded(tracked.packingStation()) && level.getBlockEntity(tracked.packingStation()) instanceof PackingStationBlockEntity point) {
                point.cancel(tracked.shipmentId());
            }
        }
        terminal.removeRequest(requestId);
    }

    /** The open requests as the GUI lists them. */
    public static List<RequestRow> rows(ServerLevel level, StorageTerminalBlockEntity terminal, List<WarehouseNetwork> warehouses) {
        List<RequestRow> rows = new ArrayList<>();
        ProductionLedger ledger = ProductionLedger.get(level);
        for (Request request : terminal.requests()) {
            int delivered = request.delivered;
            if (request.remote != null) {
                // Items of a running transfer that already arrived at this site.
                for (Tracked tracked : request.shipments) {
                    ProductionLedger.Transfer transfer = ledger.transfer(tracked.shipmentId());
                    delivered += transfer == null ? 0 : transfer.delivered();
                }
            }
            rows.add(new RequestRow(request.id, request.key.stack(1), request.requested, Math.min(request.requested, delivered),
                    request.missing, state(level, terminal, request, warehouses).name()));
        }
        return rows;
    }

    static RequestState state(ServerLevel level, StorageTerminalBlockEntity terminal, Request request, List<WarehouseNetwork> warehouses) {
        if (request.remote != null) {
            if (!request.shipped) {
                return RequestState.WAKING;
            }
            for (Tracked tracked : request.shipments) {
                if (level.isLoaded(tracked.packingStation()) && level.getBlockEntity(tracked.packingStation()) instanceof PackingStationBlockEntity point
                        && point.isSending(tracked.shipmentId())) {
                    return RequestState.SENDING;
                }
            }
            return request.shipments.isEmpty() ? RequestState.MISSING : RequestState.PICKING;
        }
        for (Tracked tracked : request.shipments) {
            if (level.isLoaded(tracked.packingStation()) && level.getBlockEntity(tracked.packingStation()) instanceof PackingStationBlockEntity point
                    && point.isSending(tracked.shipmentId())) {
                // Packets that find no room wait at the terminal.
                boolean fits = terminal.output().insertItemStacked(request.key.stack(1), true).isEmpty();
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
