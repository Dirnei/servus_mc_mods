package io.github.dirnei.mc.servus.storage.site;

import io.github.dirnei.mc.servus.foundation.config.ServusFoundationConfig;
import io.github.dirnei.mc.servus.foundation.machine.anchor.EnderAnchorData;
import io.github.dirnei.mc.servus.storage.controller.WarehouseControllerBlockEntity;
import io.github.dirnei.mc.servus.storage.network.WarehouseNetwork;
import io.github.dirnei.mc.servus.storage.power.WakeCost;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * The sites being woken right now. A wake loads every chunk of a site's
 * recorded span with a ticket of {@link SiteTickets}, counts those chunks in
 * its team's chunk load of the Ender Anchor cap while awake, and releases both
 * after the work and the grace time. Server thread only.
 */
public final class SiteWakes {

    private static final String FTB_CHUNKS = "ftbchunks";
    private static final Map<String, Wake> WAKES = new LinkedHashMap<>();

    /** One site being woken. */
    private static final class Wake {
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final WakeMachine machine = new WakeMachine();
        final Set<Object> holds = new HashSet<>();
        final Set<Long> forced = new HashSet<>();
        boolean requested;
        UUID registered;
        /** The span the team load holds for this wake, to follow a span that changes while awake. */
        Set<Long> registeredSpan;
        /** Game time of the last tick this wake was paid for; a wake pays once per tick. */
        long paidAt = Long.MIN_VALUE;

        Wake(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos.immutable();
        }
    }

    /** What a wake looks like from outside. */
    public record Status(WakeMachine.State state, WakeAllowance.Reason reason, int graceLeft, int forced) {
        public boolean awake() {
            return state == WakeMachine.State.AWAKE || state == WakeMachine.State.GRACE;
        }
    }

    private SiteWakes() {
    }

    private static Wake wake(ResourceKey<Level> dimension, BlockPos pos) {
        return WAKES.computeIfAbsent(SiteRecord.key(dimension, pos), key -> new Wake(dimension, pos));
    }

    /**
     * Asks the site whose controller is at that position to wake now. The
     * answer is the wake's state right after this request was looked at, or
     * empty when no site is recorded there.
     */
    public static Optional<Status> request(ServerLevel level, BlockPos controller) {
        if (SiteRegistry.of(level).site(level.dimension(), controller).isEmpty()) {
            return Optional.empty();
        }
        Wake wake = wake(level.dimension(), controller);
        wake.requested = true;
        step(level.getServer(), wake);
        return Optional.of(status(wake));
    }

    /** Keeps the site awake until {@link #release(Object)} is called with the same token. */
    public static Optional<Status> hold(ServerLevel level, BlockPos controller, Object token) {
        if (SiteRegistry.of(level).site(level.dimension(), controller).isEmpty()) {
            return Optional.empty();
        }
        Wake wake = wake(level.dimension(), controller);
        boolean fresh = wake.holds.add(token);
        if (fresh && !wake.machine.awake()) {
            step(level.getServer(), wake);
        }
        return Optional.of(status(wake));
    }

    /** Drops a hold; the site sleeps after its work and the grace time once nothing else holds it. */
    public static void release(Object token) {
        WAKES.values().forEach(wake -> wake.holds.remove(token));
    }

    /** The wake of the site at that position, empty while nobody wakes it. */
    public static Optional<Status> status(ResourceKey<Level> dimension, BlockPos controller) {
        return Optional.ofNullable(WAKES.get(SiteRecord.key(dimension, controller))).map(SiteWakes::status);
    }

    public static boolean isAwake(ResourceKey<Level> dimension, BlockPos controller) {
        return status(dimension, controller).map(Status::awake).orElse(false);
    }

    private static Status status(Wake wake) {
        return new Status(wake.machine.state(), wake.machine.reason(), wake.machine.graceLeft(), wake.forced.size());
    }

    /** The controller GUI's wake line, empty while nobody wakes the site. */
    public static Optional<Component> line(ResourceKey<Level> dimension, BlockPos controller) {
        return status(dimension, controller).flatMap(status -> switch (status.state()) {
            case AWAKE -> Optional.of(Component.translatable("gui.servus_storage.controller.wake.awake"));
            case GRACE -> Optional.of(Component.translatable("gui.servus_storage.controller.wake.grace", (status.graceLeft() + 19) / 20));
            case WAITING -> Optional.of(Component.translatable("gui.servus_storage.controller.wake.waiting", reason(status.reason())));
            case ASLEEP -> Optional.empty();
        });
    }

    /** The words for a reason a site does not wake. */
    public static Component reason(WakeAllowance.Reason reason) {
        return Component.translatable("gui.servus_storage.wake.reason." + reason.name().toLowerCase(Locale.ROOT));
    }

    /** A controller that left the world takes its wake along. */
    public static void forget(ServerLevel level, BlockPos controller) {
        Wake wake = WAKES.remove(SiteRecord.key(level.dimension(), controller));
        if (wake != null) {
            unload(level, wake);
        }
    }

    /** Once per server tick. */
    public static void tick(MinecraftServer server) {
        if (WAKES.isEmpty()) {
            return;
        }
        for (Wake wake : new ArrayList<>(WAKES.values())) {
            step(server, wake);
        }
    }

    /** Releases every wake while the levels still exist; nothing of a wake is kept across a restart. */
    public static void releaseAll(MinecraftServer server) {
        for (Wake wake : WAKES.values()) {
            ServerLevel level = server.getLevel(wake.dimension);
            if (level != null) {
                unload(level, wake);
            }
        }
        WAKES.clear();
    }

    /** Team load a wake held when the server stopped without releasing it is freed on start. */
    public static void clearTeamSlots(MinecraftServer server) {
        WAKES.clear();
        EnderAnchorData data = EnderAnchorData.get(server);
        for (SiteRecord site : SiteRegistry.get(server).all()) {
            ServerLevel level = server.getLevel(site.dimension());
            if (level != null && site.team().isPresent()) {
                data.unregister(site.team().get(), level, site.pos());
            }
        }
    }

    private static boolean ftbBlocks() {
        return ServusFoundationConfig.enderAnchorRespectsFtbChunks() && ModList.get().isLoaded(FTB_CHUNKS);
    }

    private static void step(MinecraftServer server, Wake wake) {
        ServerLevel level = server.getLevel(wake.dimension);
        Optional<SiteRecord> record = level == null ? Optional.empty() : SiteRegistry.get(server).site(wake.dimension, wake.pos);
        if (record.isEmpty()) {
            if (level != null) {
                unload(level, wake);
            }
            WAKES.remove(SiteRecord.key(wake.dimension, wake.pos));
            return;
        }
        SiteRecord site = record.get();
        boolean wanted = wake.requested || !wake.holds.isEmpty();
        wake.requested = false;
        WakeAllowance.Reason allowance = allowance(server, level, site, wake);
        WakeMachine.State before = wake.machine.state();
        WakeMachine.Action action = wake.machine.tick(wanted, busy(level, wake.pos), allowance, StorageStatsManager.INSTANCE.wakeGraceTicks());
        if (action == WakeMachine.Action.LOAD) {
            wake.registered = site.team().orElseThrow();
            wake.registeredSpan = null;
            register(level, wake, site.span());
        } else if (action == WakeMachine.Action.RELEASE) {
            unload(level, wake);
        }
        if (wake.machine.awake() && !pay(level, wake, site)) {
            unload(level, wake);
        }
        if (wake.machine.awake()) {
            follow(level, wake, site.span());
            register(level, wake, site.span());
        }
        if (before != wake.machine.state() && level.isLoaded(wake.pos)
                && level.getBlockEntity(wake.pos) instanceof WarehouseControllerBlockEntity controller) {
            controller.refreshSiteLamp();
        }
        if (wake.machine.state() == WakeMachine.State.ASLEEP && wake.holds.isEmpty()) {
            WAKES.remove(SiteRecord.key(wake.dimension, wake.pos));
        }
    }

    private static WakeAllowance.Reason allowance(MinecraftServer server, ServerLevel level, SiteRecord site, Wake wake) {
        EnderAnchorData data = EnderAnchorData.get(server);
        UUID team = site.team().orElse(null);
        boolean registered = team != null && data.isRegistered(team, level, wake.pos);
        // An awake site already counts in the team load.
        boolean fits = team != null && !registered && data.fits(team, EnderAnchorData.key(level, wake.pos),
                EnderAnchorData.chunks(level.dimension(), site.span()), ServusFoundationConfig.maxLoadedChunksPerTeam());
        // A loaded controller tells its FE; a sleeping one is known by its record.
        long stored = controller(level, wake.pos).map(controller -> (long) controller.energyStored()).orElse((long) site.energy());
        long toStart = WakeCost.toStart(site.span().size(), StorageStatsManager.INSTANCE.wakeFePerChunkPerTick());
        return WakeAllowance.check(team != null, ftbBlocks(), site.capacityState(), registered, fits, stored, toStart);
    }

    /**
     * Pays one tick of an awake wake from the woken site's controller: FE per
     * chunk of the site. A tick is paid once, however often the wake is looked
     * at; while the controller is not loaded yet nothing is paid. A refused
     * tick ends the wake with the reason no power.
     *
     * @return false when the wake had to end
     */
    private static boolean pay(ServerLevel level, Wake wake, SiteRecord site) {
        long now = level.getGameTime();
        if (wake.paidAt == now) {
            return true;
        }
        wake.paidAt = now;
        long cost = WakeCost.perTick(site.span().size(), StorageStatsManager.INSTANCE.wakeFePerChunkPerTick());
        Optional<WarehouseControllerBlockEntity> controller = controller(level, wake.pos);
        if (cost <= 0 || controller.isEmpty() || controller.get().drawEnergy(cost)) {
            return true;
        }
        wake.machine.stop(WakeAllowance.Reason.NO_POWER);
        return false;
    }

    private static Optional<WarehouseControllerBlockEntity> controller(ServerLevel level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof WarehouseControllerBlockEntity controller
                ? Optional.of(controller) : Optional.empty();
    }

    /** Whether the woken site's own work still runs; a controller that is not loaded yet has none. */
    private static boolean busy(ServerLevel level, BlockPos pos) {
        return controller(level, pos).flatMap(WarehouseControllerBlockEntity::network)
                .filter(network -> network.status() == WarehouseNetwork.Status.ONLINE)
                .map(WarehouseNetwork::siteBusy).orElse(false);
    }

    /** Forces the chunks of the span that are not forced yet and lets go of those that left it. */
    private static void follow(ServerLevel level, Wake wake, Set<Long> span) {
        for (long key : span) {
            if (wake.forced.add(key)) {
                force(level, wake, key, true);
            }
        }
        List<Long> left = wake.forced.stream().filter(key -> !span.contains(key)).toList();
        for (long key : left) {
            force(level, wake, key, false);
            wake.forced.remove(key);
        }
    }

    private static void force(ServerLevel level, Wake wake, long chunk, boolean add) {
        SiteTickets.CONTROLLER.forceChunk(level, wake.pos, SiteChunks.x(chunk), SiteChunks.z(chunk), add, true);
    }

    /** Keeps the team load holding exactly the chunks of the span while the site is awake. */
    private static void register(ServerLevel level, Wake wake, Set<Long> span) {
        if (wake.registered == null || span.equals(wake.registeredSpan)) {
            return;
        }
        EnderAnchorData.get(level.getServer()).register(wake.registered, EnderAnchorData.key(level, wake.pos),
                EnderAnchorData.chunks(level.dimension(), span));
        wake.registeredSpan = Set.copyOf(span);
    }

    private static void unload(ServerLevel level, Wake wake) {
        for (long key : wake.forced) {
            force(level, wake, key, false);
        }
        wake.forced.clear();
        if (wake.registered != null) {
            EnderAnchorData.get(level.getServer()).unregister(wake.registered, level, wake.pos);
            wake.registered = null;
            wake.registeredSpan = null;
        }
    }
}
