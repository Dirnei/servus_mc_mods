package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.rail.ElevatorBlockEntity;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;
import io.github.dirnei.mc.servus.shuttles.stats.ShuttlesStatsManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The lift cars of a fleet, one per shaft keyed by the shaft's lowest
 * segment, and how shuttles call, ride and cross them.
 */
final class ShuttleLifts {

    private final ShuttleFleet fleet;
    private final ShuttleFleet.Host host;
    private final Map<BlockPos, LiftCar> cars = new LinkedHashMap<>();
    /** Bottoms of the cars that moved on the last tick, to notice starts and stops. */
    private final Set<BlockPos> moving = new HashSet<>();

    ShuttleLifts(ShuttleFleet fleet, ShuttleFleet.Host host) {
        this.fleet = fleet;
        this.host = host;
    }

    LiftCar car(BlockPos bottom) {
        return cars.get(bottom);
    }

    void absorb(ShuttleLifts other) {
        cars.putAll(other.cars);
        other.cars.clear();
    }

    /** Moves every car one tick and syncs those that started or stopped; true while any moves. */
    boolean tick() {
        boolean anyMoving = false;
        for (Map.Entry<BlockPos, LiftCar> entry : cars.entrySet()) {
            BlockPos bottom = entry.getKey();
            LiftCar car = entry.getValue();
            car.tick(speed(car), accel(car));
            if (car.moving() != moving.contains(bottom)) {
                if (car.moving()) {
                    moving.add(bottom);
                } else {
                    moving.remove(bottom);
                    parkOnBlock(bottom, car);
                }
                send(bottom, car);
            }
            anyMoving |= car.moving();
        }
        return anyMoving;
    }

    /** Keeps, resizes, drops and creates cars to match the shafts; a shaft that lost segments breaks its lift. */
    void refresh(List<Shaft> shafts) {
        Map<BlockPos, Shaft> byBottom = new LinkedHashMap<>();
        for (Shaft shaft : shafts) {
            byBottom.put(Conversions.toBlockPos(shaft.bottom()), shaft);
        }
        for (Map.Entry<BlockPos, LiftCar> entry : new ArrayList<>(cars.entrySet())) {
            BlockPos bottom = entry.getKey();
            LiftCar car = entry.getValue();
            Shaft shaft = byBottom.get(bottom);
            if (shaft == null) {
                cars.remove(bottom);
                fleet.liftBroken(bottom);
            } else if (shaft.topY() < car.topY()) {
                car.resize(shaft.bottomY(), shaft.topY());
                fleet.liftBroken(bottom);
            } else if (shaft.topY() != car.topY()) {
                car.resize(shaft.bottomY(), shaft.topY());
            }
        }
        for (Map.Entry<BlockPos, Shaft> entry : byBottom.entrySet()) {
            if (!cars.containsKey(entry.getKey())) {
                cars.put(entry.getKey(), new LiftCar(entry.getValue().bottomY(), entry.getValue().topY()));
            }
        }
        moving.retainAll(cars.keySet());
        for (Map.Entry<BlockPos, LiftCar> entry : cars.entrySet()) {
            parkOnBlock(entry.getKey(), entry.getValue());
            entry.getValue().setUpgrades(upgradesAt(entry.getKey()));
        }
    }

    /** The Speed upgrades kept in the shaft's lowest segment; an unloaded one counts none until it loads. */
    private int upgradesAt(BlockPos bottom) {
        if (!host.level().isLoaded(bottom)) {
            return 0;
        }
        return host.level().getBlockEntity(bottom) instanceof ElevatorBlockEntity segment ? segment.upgradeCount() : 0;
    }

    private double speed(LiftCar car) {
        return LiftUpgrades.speed(host.liftSpeed(), ShuttlesStatsManager.INSTANCE.liftSpeedPerUpgrade(), car.upgrades());
    }

    private double accel(LiftCar car) {
        ShuttlesStatsManager stats = ShuttlesStatsManager.INSTANCE;
        return LiftUpgrades.accel(stats.liftAccel(), stats.liftAccelPerUpgrade(), car.upgrades());
    }

    /** Forgets the shuttle's call, ride or crossing. */
    void release(Shuttle shuttle) {
        if (shuttle.lift != null) {
            LiftCar car = cars.get(shuttle.lift);
            if (car != null) {
                car.remove(shuttle.id);
            }
        }
        shuttle.lift = null;
        shuttle.crossing = false;
        shuttle.inside = false;
        shuttle.riding = false;
        shuttle.handover = 0;
        shuttle.handedOver = false;
    }

    private LiftCar carFor(Shaft shaft) {
        return shaft == null ? null : cars.get(Conversions.toBlockPos(shaft.bottom()));
    }

    private static boolean inShaft(BlockPos bottom, LiftCar car, NodePos node) {
        return node.x() == bottom.getX() && node.z() == bottom.getZ() && node.y() >= car.bottomY() && node.y() <= car.topY();
    }

    /** Notes when a shuttle got onto a shaft and lets the car go once it is off again. */
    void observe(Shuttle shuttle, NodePos at) {
        if (shuttle.lift == null) {
            return;
        }
        LiftCar car = cars.get(shuttle.lift);
        if (car == null) {
            release(shuttle);
            return;
        }
        if (inShaft(shuttle.lift, car, at)) {
            shuttle.inside = true;
        } else if (shuttle.inside) {
            release(shuttle);
        }
    }

    /**
     * The gate before each path segment: shuttles wait for the car before a
     * ride, wait for a folded deck before a crossing, and never drive up or
     * down a shaft by themselves.
     */
    boolean mayStart(Shuttle shuttle, int segment) {
        List<NodePos> path = shuttle.motion.path();
        observe(shuttle, path.get(segment));
        return switch (ShaftSteps.classify(path, segment, host::shaftOf)) {
            case ENTER_RIDE -> mayEnterRide(shuttle, path.get(segment + 1));
            case ENTER_CROSS -> mayEnterCross(shuttle, path.get(segment + 1));
            case RIDE -> {
                startRide(shuttle, path, segment);
                yield false;
            }
            default -> true;
        };
    }

    private boolean mayEnterRide(Shuttle shuttle, NodePos entry) {
        Shaft shaft = host.shaftOf(entry);
        LiftCar car = carFor(shaft);
        if (car == null) {
            shuttle.gate = waitAt(StandReason.Kind.LIFT, shaft, entry);
            return false;
        }
        if (isRider(car, shuttle)) {
            return true;
        }
        bind(shuttle, shaft, false);
        car.call(shuttle.id, entry.y());
        if (car.canBoard(shuttle.id, entry.y())) {
            car.board(shuttle.id);
            fleet.markSync();
            return true;
        }
        shuttle.gate = waitAt(StandReason.Kind.LIFT, shaft, entry);
        return false;
    }

    private boolean mayEnterCross(Shuttle shuttle, NodePos entry) {
        Shaft shaft = host.shaftOf(entry);
        LiftCar car = carFor(shaft);
        if (car == null) {
            shuttle.gate = waitAt(StandReason.Kind.DECK, shaft, entry);
            return false;
        }
        if (car.isCrossing(shuttle.id)) {
            return true;
        }
        if (car.canCross(entry.y())) {
            bind(shuttle, shaft, true);
            car.beginCross(shuttle.id, entry.y());
            return true;
        }
        shuttle.gate = waitAt(StandReason.Kind.DECK, shaft, entry);
        return false;
    }

    /** The wait at that segment of the shaft for its car or a deck; without a shaft the segment stands for it. */
    private static StandReason waitAt(StandReason.Kind kind, Shaft shaft, NodePos segment) {
        return StandReason.atShaft(kind, shaft == null ? segment : shaft.bottom(), segment.y());
    }

    /**
     * A ride segment: boards the car, calling it to this floor first when the
     * leg starts without it, then sends it to the ride's last floor after the
     * handover pause. The car carries the shuttle, so the segment never starts.
     */
    private void startRide(Shuttle shuttle, List<NodePos> path, int segment) {
        NodePos from = path.get(segment);
        Shaft shaft = host.shaftOf(from);
        LiftCar car = carFor(shaft);
        if (car == null) {
            shuttle.gate = waitAt(StandReason.Kind.LIFT, shaft, from);
            return;
        }
        if (!isRider(car, shuttle)) {
            bind(shuttle, shaft, false);
            shuttle.inside = true;
            car.call(shuttle.id, from.y());
            if (!car.canBoard(shuttle.id, from.y())) {
                shuttle.gate = waitAt(StandReason.Kind.LIFT, shaft, from);
                return;
            }
            car.board(shuttle.id);
        }
        bind(shuttle, shaft, false);
        shuttle.inside = true;
        if (handover(shuttle)) {
            return;
        }
        shuttle.rideEnd = ShaftSteps.rideEnd(path, segment);
        car.send(shuttle.id, path.get(shuttle.rideEnd).y());
        shuttle.riding = true;
        fleet.markSync();
    }

    private static boolean isRider(LiftCar car, Shuttle shuttle) {
        return car.rider() != null && car.rider() == shuttle.id;
    }

    /**
     * Whether the shuttle still has to pause before the car takes it or
     * after it set it down; starts the pause the first time it is asked.
     */
    private boolean handover(Shuttle shuttle) {
        if (shuttle.handedOver) {
            shuttle.handedOver = false;
            return false;
        }
        int ticks = ShuttlesStatsManager.INSTANCE.liftHandoverTicks();
        if (ticks <= 0) {
            return false;
        }
        shuttle.handover = ticks;
        fleet.markSync();
        return true;
    }

    private void bind(Shuttle shuttle, Shaft shaft, boolean crossing) {
        BlockPos bottom = Conversions.toBlockPos(shaft.bottom());
        if (!bottom.equals(shuttle.lift)) {
            release(shuttle);
            shuttle.lift = bottom;
        }
        shuttle.crossing = crossing;
    }

    /** Whether the car has brought the riding shuttle to its floor; moves it to the exit node when so. */
    boolean rideOver(Shuttle shuttle) {
        LiftCar car = shuttle.lift == null ? null : cars.get(shuttle.lift);
        if (car == null) {
            shuttle.riding = false;
            return true;
        }
        int floor = shuttle.motion.path().get(shuttle.rideEnd).y();
        if (!car.arrivedFor(shuttle.id, floor) || handover(shuttle)) {
            return false;
        }
        shuttle.motion.skipTo(shuttle.rideEnd);
        shuttle.riding = false;
        fleet.markSync();
        return true;
    }

    void sendAll() {
        for (Map.Entry<BlockPos, LiftCar> entry : cars.entrySet()) {
            send(entry.getKey(), entry.getValue());
        }
    }

    private void send(BlockPos bottom, LiftCar car) {
        Integer target = car.target();
        ShuttleView.Drive drive = car.moving()
                ? new ShuttleView.Drive((float) car.velocity(), (float) speed(car), (float) accel(car), 0)
                : ShuttleView.Drive.STILL;
        PacketDistributor.sendToPlayersTrackingChunk(host.level(), ChunkPos.containing(bottom), new LiftSyncPayload(bottom, car.topY(),
                (float) car.y(), target == null ? (int) Math.round(car.y()) : target, drive, car.carrying()));
    }

    /** Leaves the parked car's height on the lowest segment, for players who load the chunk later. */
    private void parkOnBlock(BlockPos bottom, LiftCar car) {
        if (host.level().getBlockEntity(bottom) instanceof ElevatorBlockEntity segment) {
            segment.parkLift((float) car.y());
        }
    }
}
