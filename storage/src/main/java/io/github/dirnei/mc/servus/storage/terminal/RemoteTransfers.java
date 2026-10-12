package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.ReceiverChoice;
import io.github.dirnei.mc.servus.storage.site.RemoteSite;
import io.github.dirnei.mc.servus.storage.site.SiteCapacity;
import io.github.dirnei.mc.servus.storage.site.SiteRecord;
import io.github.dirnei.mc.servus.storage.site.SiteRegistry;
import io.github.dirnei.mc.servus.storage.site.SiteWakes;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity.Request;
import io.github.dirnei.mc.servus.storage.terminal.StorageTerminalBlockEntity.Tracked;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Transfers from other sites, requested at a Storage Terminal: the other site
 * ships from one of its Packing Stations over the wireless line into a Packing
 * Station of this site, where the items are put away. Both controllers must be
 * wireless capable. A sleeping site that can wake is held awake while the
 * request is open. Server thread only.
 */
public final class RemoteTransfers {

    /** The wake hold of one remote request; equal for the same terminal and request, also after a restart. */
    public record Hold(BlockPos terminal, int requestId) {
    }

    /** Retries for a site that answers out of stock right after it came online. */
    private static final int WAKE_TRIES = 3;

    private RemoteTransfers() {
    }

    /** The controller of the first warehouse the terminal reaches: this site's antenna. */
    public static Optional<WarehouseControllerBlockEntity> controller(ServerLevel level, BlockPos terminal) {
        return TerminalRequests.first(level, terminal).flatMap(WarehouseNetwork::controller);
    }

    /** The team whose sites the terminal sees: that of the first warehouse it reaches. */
    public static Optional<UUID> team(ServerLevel level, BlockPos terminal) {
        return controller(level, terminal).flatMap(WarehouseControllerBlockEntity::team);
    }

    /** Whether this site's controller is wireless capable, so other sites are shown and can be asked. */
    public static boolean capable(ServerLevel level, BlockPos terminal) {
        return controller(level, terminal).map(WarehouseControllerBlockEntity::capable).orElse(false);
    }

    /** The sites the terminal reaches itself, by key. */
    private static Set<String> local(ServerLevel level, BlockPos terminal) {
        return TerminalWarehouses.reachable(level, terminal).stream()
                .flatMap(warehouse -> warehouse.controllerPos().stream())
                .map(pos -> SiteRecord.key(level.dimension(), pos))
                .collect(Collectors.toSet());
    }

    /** The site's warehouse when its controller is loaded and online. Never loads a chunk. */
    public static Optional<WarehouseNetwork> network(ServerLevel level, SiteRecord site) {
        if (!site.dimension().equals(level.dimension()) || !level.isLoaded(site.pos())
                || !(level.getBlockEntity(site.pos()) instanceof WarehouseControllerBlockEntity controller)) {
            return Optional.empty();
        }
        return controller.network().filter(network -> network.status() == WarehouseNetwork.Status.ONLINE);
    }

    public static RemoteSite.State state(ServerLevel level, SiteRecord site) {
        return RemoteSite.state(network(level, site).isPresent(), site.capacityState());
    }

    /** Whether a site's controller is wireless capable: live while it is online, else as the record kept it. */
    public static boolean canTrade(ServerLevel level, SiteRecord site) {
        return network(level, site).flatMap(WarehouseNetwork::controller).map(WarehouseControllerBlockEntity::capable)
                .orElse(site.canTrade());
    }

    /**
     * The other sites of the team in this dimension whose controller holds a
     * Wireless Upgrade, powered or not, by name; empty while this site's
     * controller is not wireless capable, so the terminal shows only this site.
     */
    public static List<SiteRow> sites(ServerLevel level, BlockPos terminal) {
        Optional<WarehouseControllerBlockEntity> here = controller(level, terminal);
        if (here.isEmpty() || here.get().team().isEmpty() || !here.get().capable()) {
            return List.of();
        }
        UUID team = here.get().team().get();
        Set<String> local = local(level, terminal);
        List<SiteRow> rows = new ArrayList<>();
        for (SiteRecord site : SiteRegistry.of(level).ofTeam(team, level.dimension())) {
            if (!local.contains(site.key()) && site.hasUpgrade()) {
                rows.add(new SiteRow(site.key(), site.name(), state(level, site).ordinal(), canTrade(level, site)));
            }
        }
        return rows;
    }

    /** The stock of a site: live while it is online, else what it last reported. */
    public static List<Map.Entry<ItemKey, Long>> stock(ServerLevel level, SiteRecord site) {
        Optional<WarehouseNetwork> network = network(level, site);
        if (network.isPresent()) {
            return network.get().index().entries();
        }
        List<Map.Entry<ItemKey, Long>> list = new ArrayList<>(site.stock().entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return Collections.unmodifiableList(list);
    }

    /**
     * The Packing Station of this site that receives a transfer: on the first
     * reachable warehouse, the one with the fewest open incoming transfers.
     */
    private static Optional<PackingStationBlockEntity> receiver(ServerLevel level, BlockPos terminal) {
        Optional<WarehouseNetwork> first = TerminalRequests.first(level, terminal);
        if (first.isEmpty()) {
            return Optional.empty();
        }
        List<PackingStationBlockEntity> stations = new ArrayList<>();
        for (BlockPos pos : first.get().packingStations()) {
            if (level.getBlockEntity(pos) instanceof PackingStationBlockEntity point) {
                stations.add(point);
            }
        }
        return ReceiverChoice.choose(stations, point -> first.get().incomingWireless(point.getBlockPos()),
                point -> point.getBlockPos().asLong());
    }

    /** Why this site cannot receive now, empty when it can: no upgrade, no power or no Packing Station. */
    private static String hereRefusal(ServerLevel level, BlockPos terminal) {
        Optional<WarehouseControllerBlockEntity> here = controller(level, terminal);
        if (here.isEmpty() || !here.get().hasWirelessUpgrade()) {
            return "no_wireless_here";
        }
        if (!here.get().capable()) {
            return "no_power_here";
        }
        return receiver(level, terminal).isPresent() ? "" : "no_packing_here";
    }

    /** Whether this site's controller lost the upgrade or its power, so its open transfers wait for power. */
    static boolean receiverUnpowered(ServerLevel level, BlockPos terminal) {
        return !capable(level, terminal);
    }

    /** Why a site cannot send, empty when it can: live while online, else from its record. */
    private static String thereRefusal(ServerLevel level, SiteRecord site) {
        Optional<WarehouseNetwork> network = network(level, site);
        if (network.isPresent()) {
            Optional<WarehouseControllerBlockEntity> controller = network.get().controller();
            if (controller.isEmpty() || !controller.get().hasWirelessUpgrade()) {
                return "no_wireless_there";
            }
            if (!controller.get().capable()) {
                return "no_power_there";
            }
            return network.get().packingStations().isEmpty() ? "no_packing_there" : "";
        }
        if (!site.hasUpgrade()) {
            return "no_wireless_there";
        }
        // A sleeping controller keeps its FE: one that slept unpowered cannot send when it wakes.
        if (!site.powered()) {
            return "no_power_there";
        }
        return site.hasPacking() ? "" : "no_packing_there";
    }

    /**
     * Requests {@code amount} of {@code key} from the site with that key.
     *
     * @return an empty string when the transfer was made, else the refusal key:
     *         {@code offline}, {@code no_team}, {@code no_site}, {@code no_wireless_here}, {@code no_power_here},
     *         {@code no_packing_here}, {@code no_wireless_there}, {@code no_power_there}, {@code no_packing_there},
     *         {@code site_asleep} or a refusal of the other site's shipment
     */
    public static String request(ServerLevel level, StorageTerminalBlockEntity terminal, String siteKey, ItemKey key, int amount) {
        if (TerminalWarehouses.reachable(level, terminal.getBlockPos()).isEmpty()) {
            return "offline";
        }
        Optional<UUID> team = team(level, terminal.getBlockPos());
        if (team.isEmpty()) {
            return "no_team";
        }
        Optional<WarehouseControllerBlockEntity> here = controller(level, terminal.getBlockPos());
        if (here.isEmpty() || !here.get().hasWirelessUpgrade()) {
            return "no_wireless_here";
        }
        if (!here.get().capable()) {
            return "no_power_here";
        }
        Optional<SiteRecord> site = SiteRegistry.of(level).site(siteKey);
        if (site.isEmpty() || !site.get().dimension().equals(level.dimension()) || !team.get().equals(site.get().team().orElse(null))
                || local(level, terminal.getBlockPos()).contains(siteKey)) {
            return "no_site";
        }
        String hereRefusal = hereRefusal(level, terminal.getBlockPos());
        if (!hereRefusal.isEmpty()) {
            return hereRefusal;
        }
        String thereRefusal = thereRefusal(level, site.get());
        if (!thereRefusal.isEmpty()) {
            return thereRefusal;
        }
        if (!state(level, site.get()).orderable()) {
            return "site_asleep";
        }
        if (network(level, site.get()).filter(network -> network.available(key) <= 0).isPresent()) {
            return "out_of_stock";
        }
        Request request = terminal.newRequest(level, key, amount);
        request.remote = siteKey;
        request.missing = amount;
        if (network(level, site.get()).isPresent()) {
            // An online site answers now; only a site that still has to wake gets retries.
            request.tries = WAKE_TRIES;
        }
        String refusal = ship(level, terminal, request);
        if (request.shipped && request.shipments.isEmpty()) {
            terminal.removeRequest(request.id);
            return refusal.isEmpty() ? "out_of_stock" : refusal;
        }
        terminal.setChanged();
        return "";
    }

    /**
     * Ships the request from its site if the site is online; holds a site that
     * sleeps but can wake. Marks the request shipped once the online site was asked.
     * While this site cannot receive (no upgrade, no power, no Packing Station)
     * the request waits.
     */
    static String ship(ServerLevel level, StorageTerminalBlockEntity terminal, Request request) {
        Optional<SiteRecord> site = SiteRegistry.of(level).site(request.remote);
        if (site.isEmpty() || team(level, terminal.getBlockPos()).isEmpty()) {
            request.shipped = true;
            return "no_site";
        }
        String hereRefusal = hereRefusal(level, terminal.getBlockPos());
        if (!hereRefusal.isEmpty()) {
            // Waits until this site can receive again; the row says waiting for power.
            return hereRefusal;
        }
        PackingStationBlockEntity receiver = receiver(level, terminal.getBlockPos()).orElseThrow();
        Hold hold = new Hold(terminal.getBlockPos(), request.id);
        boolean wakeable = site.get().capacityState() == SiteCapacity.State.COVERED;
        Optional<WarehouseNetwork> remote = network(level, site.get());
        if (remote.isEmpty()) {
            String thereRefusal = thereRefusal(level, site.get());
            if (!thereRefusal.isEmpty()) {
                request.shipped = true;
                return thereRefusal;
            }
            if (!wakeable) {
                request.shipped = true;
                return "site_asleep";
            }
            SiteWakes.hold(level, site.get().pos(), hold);
            return "";
        }
        if (wakeable) {
            // Keeps the site loaded for the shuttles and its Packing Station until the request leaves the list.
            SiteWakes.hold(level, site.get().pos(), hold);
        }
        String here = controller(level, terminal.getBlockPos()).map(controller -> SiteRecord.key(level.dimension(), controller.getBlockPos()))
                .orElse("");
        WarehouseNetwork.Shipping shipping = remote.get().shipWireless(receiver.getBlockPos(), here, request.key, request.missing, request.id);
        if (!shipping.shipped() && request.tries < WAKE_TRIES) {
            // A site that just woke may not have joined its stations or indexed its storage yet: ask again with the next retry.
            request.tries++;
            return "";
        }
        request.shipped = true;
        if (!shipping.shipped()) {
            return shipping.refusal();
        }
        PackingStationBlockEntity.Shipment shipment = shipping.shipment().orElseThrow();
        request.shipments.add(new Tracked(shipping.packingStation(), shipment.id(), shipment.planned()));
        request.missing = Math.max(0, request.missing - shipment.planned());
        terminal.setChanged();
        return "";
    }

    /** Lets the site of a remote request sleep again. */
    public static void release(BlockPos terminal, int requestId) {
        SiteWakes.release(new Hold(terminal, requestId));
    }

    /** Drops the queued picks of a remote request at its site, when that site is online. */
    static void dropQueued(ServerLevel level, Request request) {
        SiteRegistry.of(level).site(request.remote).flatMap(site -> network(level, site))
                .ifPresent(network -> network.dropQueuedFor(request.id));
    }
}
