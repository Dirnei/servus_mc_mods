package io.github.dirnei.mc.servus.shuttles.terminal;

import io.github.dirnei.mc.servus.shuttles.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import io.github.dirnei.mc.servus.shuttles.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.shuttles.cpu.CpuOrders;
import io.github.dirnei.mc.servus.shuttles.order.OrderBook;
import io.github.dirnei.mc.servus.shuttles.order.ProductionLedger;
import io.github.dirnei.mc.servus.shuttles.pattern.EncodedPattern;
import io.github.dirnei.mc.servus.foundation.data.DataNetwork;
import io.github.dirnei.mc.servus.shuttles.site.SiteRegistry;
import io.github.dirnei.mc.servus.shuttles.site.SiteWakes;
import io.github.dirnei.mc.servus.shuttles.site.WakeAllowance;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity.Request;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity.RequestState;
import io.github.dirnei.mc.servus.shuttles.terminal.StorageTerminalBlockEntity.Tracked;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;

/**
 * Requests of a Storage Terminal on the data line: shipped by the line's
 * warehouse through its Packing Stations, booked as shipments end, retried
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
            terminal.setOnline(TerminalWarehouses.online(level, terminal.getBlockPos()).isPresent());
        }
        if (ticks % RETRY_TICKS == 0) {
            retry(level, terminal);
            if (!terminal.requests().isEmpty()) {
                terminal.setChanged();
            }
        }
    }

    /**
     * Makes a request for {@code amount} of {@code key}: what is requestable
     * is shipped, the rest is crafted when a CPU of the line can make it with
     * nothing missing, else the request is only for what can be shipped.
     * When stock covers it and this site has an open request for the key
     * without a work order, that request grows instead.
     *
     * @return an empty string when the request was made, else the refusal key:
     *         {@code offline}, {@code line_conflict}, {@code no_packing_station}, {@code needs_shuttle}, {@code no_rail},
     *         {@code out_of_stock} or {@code output_full}
     */
    public static String request(ServerLevel level, StorageTerminalBlockEntity terminal, ItemKey key, int amount) {
        LineWarehouse line = TerminalWarehouses.line(level, terminal.getBlockPos());
        Optional<WarehouseNetwork> online = line.online();
        if (online.isEmpty()) {
            return unavailable(line);
        }
        WarehouseNetwork warehouse = online.get();
        long available = warehouse.available(key);
        boolean restCraftable = amount > available
                && TerminalCraftables.preview(level, terminal.getBlockPos(), key, (int) (amount - available)).startable();
        RequestSplit split = RequestSplit.of(amount, available, restCraftable);
        if (split.total() <= 0) {
            return "out_of_stock";
        }
        Optional<Request> open = split.toCraft() == 0 ? growable(level, terminal, key) : Optional.empty();
        if (open.isPresent()) {
            return grow(terminal, open.get(), warehouse, split.fromStock());
        }
        if (terminal.freeSlot(key) == RequestSlots.NONE) {
            return "output_full";
        }
        Request request = terminal.newRequest(level, key, split.total());
        String[] refusal = {""};
        int remaining = ship(terminal, request, warehouse, split.fromStock(), refusal) + split.toCraft();
        if (remaining > 0 && restCraftable) {
            CpuOrders.Placed placed = CpuOrders.create(level, terminal.getBlockPos(), key, remaining, request.id);
            if (placed.made()) {
                request.orderCpu = placed.cpu();
                request.missing = remaining;
                placed.created().order().deliverTo(terminal.getBlockPos());
                return "";
            }
        }
        if (request.shipments.isEmpty()) {
            terminal.removeRequest(request.id);
            return refusal[0].isEmpty() ? "out_of_stock" : refusal[0];
        }
        request.requested -= remaining;
        return "";
    }

    /** An open request of this site for the key whose items all come from stock. */
    private static Optional<Request> growable(ServerLevel level, StorageTerminalBlockEntity terminal, ItemKey key) {
        return terminal.requests().stream()
                .filter(request -> request.remote == null && request.key.equals(key) && !producing(level, request))
                .findFirst();
    }

    /** Ships {@code amount} more on an open request; returns the refusal key, empty when something was shipped. */
    private static String grow(StorageTerminalBlockEntity terminal, Request request, WarehouseNetwork warehouse, int amount) {
        String[] refusal = {""};
        int shipped = amount - ship(terminal, request, warehouse, amount, refusal);
        if (shipped <= 0) {
            return refusal[0].isEmpty() ? "out_of_stock" : refusal[0];
        }
        request.requested += shipped;
        terminal.growSlots(request);
        terminal.setChanged();
        return "";
    }

    /** Why a line has no warehouse to work with: a conflict, else offline. */
    static String unavailable(LineWarehouse line) {
        return line.conflict() ? LineWarehouse.CONFLICT_KEY : "offline";
    }

    /**
     * Crafts {@code amount} of {@code key} on a CPU of the line, independent of
     * the item's own stock, for a new request whose results land in the output
     * slots. A material pattern's item without its material crafts the variant
     * the stock holds the items of most, and the request names that variant.
     *
     * @return an empty string when the order was made, else the refusal key:
     *         {@code offline}, {@code line_conflict}, {@code output_full}, {@code no_cpu}, {@code no_producer} or {@code missing}
     */
    public static String craft(ServerLevel level, StorageTerminalBlockEntity terminal, ItemKey key, int amount) {
        LineWarehouse line = TerminalWarehouses.line(level, terminal.getBlockPos());
        if (line.online().isEmpty()) {
            return unavailable(line);
        }
        OrderBook.Preview preview =
                TerminalCraftables.preview(level, terminal.getBlockPos(), key, amount);
        if (!preview.refusal().isEmpty()) {
            return preview.refusal();
        }
        if (!preview.missing().isEmpty()) {
            return "missing";
        }
        ItemKey made = preview.made();
        if (terminal.freeSlot(made) == RequestSlots.NONE) {
            return "output_full";
        }
        Request request = terminal.newRequest(level, made, amount);
        request.missing = amount;
        CpuOrders.Placed placed = CpuOrders.create(level, terminal.getBlockPos(), made, amount, request.id);
        if (!placed.made()) {
            terminal.removeRequest(request.id);
            return placed.refusal();
        }
        request.orderCpu = placed.cpu();
        placed.created().order().deliverTo(terminal.getBlockPos());
        return "";
    }

    /**
     * Crafts {@code amount} of the pattern's result on a CPU of the line with
     * that pattern only, saved on the line or not, for a new request whose
     * results land in the output slots.
     *
     * @return an empty string when the order was made, else the refusal key:
     *         {@code offline}, {@code line_conflict}, {@code output_full}, {@code no_cpu}, {@code no_station}, {@code no_producer} or {@code missing}
     */
    public static String craftOneOff(ServerLevel level, StorageTerminalBlockEntity terminal, EncodedPattern pattern, int amount) {
        LineWarehouse warehouse = TerminalWarehouses.line(level, terminal.getBlockPos());
        if (warehouse.online().isEmpty()) {
            return unavailable(warehouse);
        }
        String refusal = TerminalCraftables.orderRefusal(level, terminal.getBlockPos());
        if (!refusal.isEmpty()) {
            return refusal;
        }
        OrderBook.Preview preview = TerminalCraftables.previewOneOff(level, terminal.getBlockPos(), pattern, amount);
        if (!preview.refusal().isEmpty()) {
            return preview.refusal();
        }
        if (!preview.missing().isEmpty()) {
            return "missing";
        }
        if (terminal.freeSlot(preview.made()) == RequestSlots.NONE) {
            return "output_full";
        }
        DataNetwork line = TerminalCraftables.line(level, terminal.getBlockPos()).orElseThrow();
        Request request = terminal.newRequest(level, preview.made(), amount);
        request.missing = amount;
        CpuOrders.Placed placed = CpuOrders.createOneOff(level, line, pattern, amount, request.id);
        if (!placed.made()) {
            terminal.removeRequest(request.id);
            return placed.refusal();
        }
        request.orderCpu = placed.cpu();
        placed.created().order().deliverTo(terminal.getBlockPos());
        return "";
    }

    /** Ships up to {@code amount} from the warehouse; returns what is still missing. */
    private static int ship(StorageTerminalBlockEntity terminal, Request request, WarehouseNetwork warehouse,
                            int amount, String[] refusal) {
        long available = warehouse.available(request.key);
        if (amount <= 0 || available <= 0) {
            return Math.max(0, amount);
        }
        WarehouseNetwork.Shipping shipping = warehouse.ship(terminal.getBlockPos(), request.key,
                (int) Math.min(available, amount), request.id);
        if (!shipping.shipped()) {
            refusal[0] = shipping.refusal();
            return amount;
        }
        PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
        request.shipments.add(new Tracked(shipping.packingStation(), shipment.id(), shipment.planned()));
        terminal.setChanged();
        return Math.max(0, amount - shipment.planned());
    }

    /**
     * Books ended shipments: what they did not bring is missing again. A
     * transfer counts only at its relay, the last leg into the output slots.
     * Drops requests that are done.
     */
    private static void book(ServerLevel level, StorageTerminalBlockEntity terminal) {
        List<Integer> done = new ArrayList<>();
        ProductionLedger ledger = ProductionLedger.get(level);
        for (Request request : terminal.requests()) {
            for (Iterator<Tracked> it = request.shipments.iterator(); it.hasNext(); ) {
                Tracked tracked = it.next();
                ProductionLedger.Transfer transfer = ledger.transfer(tracked.shipmentId());
                // A shipment the ledger forgot is shipped again rather than waited for.
                if (transfer == null || transfer.ended()) {
                    if (request.remote == null) {
                        int delivered = transfer == null ? 0 : transfer.delivered();
                        request.missing += Math.max(0, tracked.asked() - delivered);
                    }
                    it.remove();
                    terminal.setChanged();
                }
            }
            if (request.relay != null) {
                ProductionLedger.Transfer transfer = ledger.transfer(request.relay.shipmentId());
                if (transfer == null || transfer.ended()) {
                    int delivered = transfer == null ? 0 : transfer.delivered();
                    request.missing += Math.max(0, request.relay.asked() - delivered);
                    request.relay = null;
                    terminal.setChanged();
                }
            }
            if (request.relay == null && request.shipments.isEmpty()
                    && (request.delivered >= request.requested || request.remote != null && request.shipped)) {
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
        Optional<WarehouseNetwork> warehouse = null;
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
            if (warehouse == null) {
                warehouse = TerminalWarehouses.online(level, terminal.getBlockPos());
            }
            if (warehouse.isEmpty()) {
                return;
            }
            request.missing = ship(terminal, request, warehouse.get(), request.missing, new String[] {""});
        }
    }

    /** Cancels a request: queued picks and its work order go, shipments end and their items go back into storage. */
    public static void cancel(ServerLevel level, StorageTerminalBlockEntity terminal, int requestId) {
        Request request = terminal.request(requestId);
        if (request == null) {
            return;
        }
        TerminalWarehouses.online(level, terminal.getBlockPos()).ifPresent(warehouse -> warehouse.dropQueuedFor(requestId));
        if (request.remote != null) {
            RemoteTransfers.dropQueued(level, request);
        }
        CpuOrders.cancelForRequest(level, terminal.getBlockPos(), requestId);
        List<Tracked> legs = new ArrayList<>(request.shipments);
        if (request.relay != null) {
            legs.add(request.relay);
        }
        for (Tracked tracked : legs) {
            if (level.isLoaded(tracked.packingStation()) && level.getBlockEntity(tracked.packingStation()) instanceof PackingStationBlockEntity point) {
                point.cancel(tracked.shipmentId());
            }
        }
        terminal.removeRequest(requestId);
    }

    /** The open requests as the GUI lists them. */
    public static List<RequestRow> rows(ServerLevel level, StorageTerminalBlockEntity terminal, Optional<WarehouseNetwork> warehouse) {
        List<RequestRow> rows = new ArrayList<>();
        for (Request request : terminal.requests()) {
            int delivered = request.delivered;
            rows.add(new RequestRow(request.id, request.key.stack(1), request.requested, Math.min(request.requested, delivered),
                    request.missing, state(level, terminal, request, warehouse).name(), List.copyOf(request.slots)));
        }
        return rows;
    }

    private static RequestState state(ServerLevel level, StorageTerminalBlockEntity terminal, Request request, Optional<WarehouseNetwork> warehouse) {
        return request.remote != null ? remoteState(level, terminal, request) : localState(level, terminal, request, warehouse);
    }

    /** The Packing Station running a shipment, while its chunk is loaded. */
    private static Optional<PackingStationBlockEntity> station(ServerLevel level, Tracked tracked) {
        return level.isLoaded(tracked.packingStation()) && level.getBlockEntity(tracked.packingStation()) instanceof PackingStationBlockEntity point
                ? Optional.of(point) : Optional.empty();
    }

    private static boolean sending(ServerLevel level, Tracked tracked) {
        return station(level, tracked).filter(point -> point.isSending(tracked.shipmentId())).isPresent();
    }

    /** Packets that find no room in the output slots wait at the terminal. */
    private static RequestState delivering(StorageTerminalBlockEntity terminal, Request request) {
        return terminal.fits(request) ? RequestState.SENDING : RequestState.WAITING_ROOM;
    }

    /**
     * A transfer from another site waits for power while this site's
     * controller lost its upgrade or power, or the sending site's controller,
     * or this one as the sender sees it, is not wireless capable, or the
     * sending site's shuttles wait for power. Once the other site sent
     * everything, the rest comes from this site's receiving Packing Station.
     */
    private static RequestState remoteState(ServerLevel level, StorageTerminalBlockEntity terminal, Request request) {
        if (!request.shipped) {
            return RemoteTransfers.receiverUnpowered(level, terminal.getBlockPos()) || wakeWaitsForPower(level, request.remote)
                    ? RequestState.NO_POWER : RequestState.WAKING;
        }
        if (request.shipments.stream().anyMatch(tracked -> sending(level, tracked))) {
            return RequestState.SENDING;
        }
        if (!request.shipments.isEmpty() && RemoteTransfers.receiverUnpowered(level, terminal.getBlockPos())) {
            return RequestState.NO_POWER;
        }
        for (Tracked tracked : request.shipments) {
            if (station(level, tracked).filter(point -> point.waitsForPower(tracked.shipmentId())
                    || point.network().map(WarehouseNetwork::noPower).orElse(false)).isPresent()) {
                return RequestState.NO_POWER;
            }
        }
        if (request.relay != null && sending(level, request.relay)) {
            return delivering(terminal, request);
        }
        if (request.shipments.isEmpty()) {
            return request.relay == null ? RequestState.MISSING : delivering(terminal, request);
        }
        return RequestState.PICKING;
    }

    /** Shuttles and Crafting Stations stop without power: what waits for them waits for power. */
    private static RequestState localState(ServerLevel level, StorageTerminalBlockEntity terminal, Request request,
                                           Optional<WarehouseNetwork> warehouse) {
        if (request.shipments.stream().anyMatch(tracked -> sending(level, tracked))) {
            return delivering(terminal, request);
        }
        boolean noPower = warehouse.filter(WarehouseNetwork::noPower).isPresent();
        if (!request.shipments.isEmpty()) {
            // Nothing is picked for slots without room, so a full slot is the reason the rest waits.
            return noPower ? RequestState.NO_POWER : terminal.fits(request) ? RequestState.PICKING : RequestState.WAITING_ROOM;
        }
        if (CpuOrders.cpuAt(level, request.orderCpu).filter(cpu -> cpu.book().hasOpenOrderFor(request.id)).isPresent()) {
            return noPower ? RequestState.NO_POWER : RequestState.PRODUCING;
        }
        return RequestState.MISSING;
    }

    /** Whether the wake of the other site waits because its controller had too little FE to wake. */
    private static boolean wakeWaitsForPower(ServerLevel level, String siteKey) {
        return SiteRegistry.of(level).site(siteKey)
                .flatMap(site -> SiteWakes.status(site.dimension(), site.pos()))
                .map(status -> status.reason() == WakeAllowance.Reason.NO_POWER)
                .orElse(false);
    }
}
