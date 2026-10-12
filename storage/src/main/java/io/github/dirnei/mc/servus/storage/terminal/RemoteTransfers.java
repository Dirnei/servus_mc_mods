package io.github.dirnei.mc.servus.storage.terminal;

import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.ItemKey;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.packing.PackingStationBlockEntity;
import io.github.dirnei.mc.servus.storage.packing.WirelessPackingStationBlockEntity;
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
 * ships over the wireless line into a Wireless Packing Station of this site,
 * where the items are put away. A sleeping site that can wake is held awake
 * while the request is open. Server thread only.
 */
public final class RemoteTransfers {

    /** The wake hold of one remote request; equal for the same terminal and request, also after a restart. */
    public record Hold(BlockPos terminal, int requestId) {
    }

    /** Retries for a site that answers out of stock right after it came online. */
    private static final int WAKE_TRIES = 3;

    private RemoteTransfers() {
    }

    /** The team whose sites the terminal sees: that of the first warehouse it reaches. */
    public static Optional<UUID> team(ServerLevel level, BlockPos terminal) {
        return TerminalRequests.first(level, terminal).flatMap(WarehouseNetwork::controller).flatMap(WarehouseControllerBlockEntity::team);
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

    /**
     * The other sites of the team in this dimension that have a Wireless
     * Packing Station of the team, by name; a site whose stations are all
     * unpowered is listed off the wireless line, so a request there says why.
     */
    public static List<SiteRow> sites(ServerLevel level, BlockPos terminal) {
        Optional<UUID> team = team(level, terminal);
        if (team.isEmpty()) {
            return List.of();
        }
        Set<String> here = local(level, terminal);
        List<SiteRow> rows = new ArrayList<>();
        for (SiteRecord site : SiteRegistry.of(level).ofTeam(team.get(), level.dimension())) {
            if (!here.contains(site.key()) && site.hasStation(team.get())) {
                rows.add(new SiteRow(site.key(), site.name(), state(level, site).ordinal(), site.hasWireless(team.get()), true));
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

    /** The Wireless Packing Station of this site that receives transfers: powered, of the team, on the first reachable warehouse. */
    private static Optional<WirelessPackingStationBlockEntity> receiver(ServerLevel level, BlockPos terminal, UUID team) {
        return TerminalRequests.first(level, terminal)
                .flatMap(warehouse -> warehouse.wirelessStations(team).stream().filter(WirelessPackingStationBlockEntity::powered).findFirst());
    }

    /** Why this site cannot receive: {@code no_wireless_here} without a station of the team, {@code no_power_here} when none is powered. */
    private static String noReceiver(ServerLevel level, BlockPos terminal, UUID team) {
        boolean any = TerminalRequests.first(level, terminal).map(warehouse -> !warehouse.wirelessStations(team).isEmpty()).orElse(false);
        return any ? "no_power_here" : "no_wireless_here";
    }

    /** Whether this site has Wireless Packing Stations of the team but none is powered. */
    static boolean receiverUnpowered(ServerLevel level, BlockPos terminal) {
        Optional<UUID> team = team(level, terminal);
        return team.isPresent() && receiver(level, terminal, team.get()).isEmpty()
                && noReceiver(level, terminal, team.get()).equals("no_power_here");
    }

    /**
     * Requests {@code amount} of {@code key} from the site with that key.
     *
     * @return an empty string when the transfer was made, else the refusal key:
     *         {@code offline}, {@code no_team}, {@code no_site}, {@code no_wireless_here}, {@code no_power_here},
     *         {@code no_power_there}, {@code site_asleep} or a refusal of the other site's shipment
     */
    public static String request(ServerLevel level, StorageTerminalBlockEntity terminal, String siteKey, ItemKey key, int amount) {
        if (TerminalWarehouses.reachable(level, terminal.getBlockPos()).isEmpty()) {
            return "offline";
        }
        Optional<UUID> team = team(level, terminal.getBlockPos());
        if (team.isEmpty()) {
            return "no_team";
        }
        Optional<SiteRecord> site = SiteRegistry.of(level).site(siteKey);
        if (site.isEmpty() || !site.get().dimension().equals(level.dimension()) || !team.get().equals(site.get().team().orElse(null))
                || !site.get().hasStation(team.get()) || local(level, terminal.getBlockPos()).contains(siteKey)) {
            return "no_site";
        }
        if (receiver(level, terminal.getBlockPos(), team.get()).isEmpty()) {
            return noReceiver(level, terminal.getBlockPos(), team.get());
        }
        if (!site.get().hasWireless(team.get()) && !poweredNow(level, site.get(), team.get())) {
            return "no_power_there";
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
     */
    static String ship(ServerLevel level, StorageTerminalBlockEntity terminal, Request request) {
        Optional<SiteRecord> site = SiteRegistry.of(level).site(request.remote);
        Optional<UUID> team = team(level, terminal.getBlockPos());
        if (site.isEmpty() || team.isEmpty()) {
            request.shipped = true;
            return "no_site";
        }
        Optional<WirelessPackingStationBlockEntity> receiver = receiver(level, terminal.getBlockPos(), team.get());
        if (receiver.isEmpty()) {
            // Waits until this site has a powered station again; the row says no power.
            return noReceiver(level, terminal.getBlockPos(), team.get());
        }
        Hold hold = new Hold(terminal.getBlockPos(), request.id);
        boolean wakeable = site.get().capacityState() == SiteCapacity.State.COVERED;
        Optional<WarehouseNetwork> remote = network(level, site.get());
        if (remote.isEmpty()) {
            if (!site.get().hasWireless(team.get())) {
                // A sleeping site's stations keep their FE: one that slept unpowered cannot send when it wakes.
                request.shipped = true;
                return "no_power_there";
            }
            if (!wakeable) {
                request.shipped = true;
                return "site_asleep";
            }
            SiteWakes.hold(level, site.get().pos(), hold);
            return "";
        }
        if (wakeable) {
            // Keeps the site loaded for the shuttles and the wireless station until the request leaves the list.
            SiteWakes.hold(level, site.get().pos(), hold);
        }
        WarehouseNetwork.Shipping shipping = remote.get().shipWireless(receiver.get().getBlockPos(), request.key, request.missing,
                request.id, team.get());
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

    /**
     * Whether an online site has a powered station of the team right now, for
     * the tick between a station getting FE and its site record learning it.
     */
    private static boolean poweredNow(ServerLevel level, SiteRecord site, UUID team) {
        return network(level, site).map(network -> network.wirelessStations(team).stream().anyMatch(WirelessPackingStationBlockEntity::powered))
                .orElse(false);
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
