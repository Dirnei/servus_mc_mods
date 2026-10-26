package io.github.dirnei.mc.servus.shuttles.shuttle;

import net.minecraft.core.Direction;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.naming.MachineLabels;
import io.github.dirnei.mc.servus.foundation.network.Conversions;
import io.github.dirnei.mc.servus.shuttles.network.StorageNodeBlockEntity;
import io.github.dirnei.mc.servus.shuttles.registry.ServusShuttlesMachineKinds;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The Data Potato's chat report on a Warehouse Rail, Farm Rail or elevator
 * segment: each shuttle on that block or driving off it, and on a segment the
 * one the car carries at its height, with what it does now. A shuttle held
 * behind another is followed along the shuttles each waits behind. Reads
 * only; server thread only.
 */
public final class ShuttleReport {

    private static final String KEY = "message.servus_shuttles.potato.shuttle_";
    /** The most shuttles named along one chain of waiting shuttles. */
    private static final int CHAIN_LIMIT = 8;

    private ShuttleReport() {
    }

    /** The report for the rail or segment block at pos, through the warehouse it belongs to. */
    public static List<Component> of(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof StorageNodeBlockEntity node)) {
            return List.of();
        }
        return node.network().map(network -> network.shuttleReport(pos)).orElseGet(() -> List.of(Component.translatable(KEY + "offline")));
    }

    static List<Component> at(ShuttleFleet fleet, BlockPos pos) {
        List<Shuttle> all = fleet.shuttles();
        Map<Integer, Shuttle> byId = new HashMap<>();
        for (Shuttle shuttle : all) {
            byId.put(shuttle.id, shuttle);
        }
        List<Component> report = new ArrayList<>();
        for (Shuttle shuttle : all) {
            if (!at(fleet, shuttle, pos)) {
                continue;
            }
            report.add(Component.translatable(KEY + "line", label(shuttle), reason(fleet, shuttle, byId)));
            if (shuttle.reason.kind() != StandReason.Kind.FOLLOWING) {
                continue;
            }
            StandReason.Chain chain = StandReason.chain(shuttle.id, id -> leaderOf(byId.get(id)), CHAIN_LIMIT);
            for (int id : chain.ids()) {
                Shuttle next = byId.get(id);
                if (next == null) {
                    break;
                }
                report.add(Component.translatable(KEY + "chain", label(next), reason(fleet, next, byId)));
            }
            if (chain.cycle()) {
                report.add(Component.translatable(KEY + "cycle"));
            }
        }
        if (report.isEmpty()) {
            report.add(Component.translatable(KEY + "none"));
        }
        return report;
    }

    /** Whether the shuttle is out and stands on, drives off or is carried at the height of the block at pos. */
    private static boolean at(ShuttleFleet fleet, Shuttle shuttle, BlockPos pos) {
        ShuttleMotion motion = shuttle.motion;
        if (motion == null || (!shuttle.busy() || shuttle.leg == Leg.STRANDED) && motion.arrived()) {
            return false;
        }
        if (shuttle.riding && shuttle.lift != null) {
            LiftCar car = fleet.lift(shuttle.lift);
            return car != null && pos.getX() == shuttle.lift.getX() && pos.getZ() == shuttle.lift.getZ()
                    && Math.round(car.y()) == pos.getY();
        }
        return Conversions.toBlockPos(motion.currentNode()).equals(pos);
    }

    /** The id of the shuttle this one waits behind, -1 when none or when it is no longer in the fleet. */
    private static int leaderOf(Shuttle shuttle) {
        return shuttle != null && shuttle.reason.kind() == StandReason.Kind.FOLLOWING ? shuttle.reason.leader() : -1;
    }

    private static Component label(Shuttle shuttle) {
        return ShuttleItem.chatLabel(shuttle.item);
    }

    /** What the shuttle does now, in words. */
    private static Component reason(ShuttleFleet fleet, Shuttle shuttle, Map<Integer, Shuttle> byId) {
        StandReason reason = shuttle.reason;
        String key = KEY + "reason." + reason.kind().name().toLowerCase(Locale.ROOT);
        return switch (reason.kind()) {
            case FOLLOWING -> {
                Shuttle leader = byId.get(reason.leader());
                yield leader == null ? Component.translatable(KEY + "reason.following_unknown") : Component.translatable(key, label(leader));
            }
            case LIFT, DECK -> Component.translatable(key, shaftLabel(fleet, shuttle), reason.floor());
            case STOP -> stopReason(fleet, shuttle, key);
            case RIDING -> Component.translatable(key, shaftLabel(fleet, shuttle), rideFloor(fleet, shuttle));
            case FIELD_WORK -> {
                FieldRun run = shuttle.fieldRun();
                yield run == null ? Component.translatable(key) : Component.translatable(key + "_at", MachineLabels.chatAt(fleet.level(), run.head));
            }
            default -> Component.translatable(key);
        };
    }

    /** The named machine beside the rail the shuttle stops at, else the rail's coordinates. */
    private static Component stopReason(ShuttleFleet fleet, Shuttle shuttle, String key) {
        BlockPos at = Conversions.toBlockPos(shuttle.at());
        Level level = fleet.level();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos beside = at.relative(side);
            if (level.isLoaded(beside) && level.getBlockEntity(beside) instanceof NamedMachine machine) {
                return Component.translatable(key + "_at", machine.chatLabel());
            }
        }
        return Component.translatable(key, at.getX(), at.getY(), at.getZ());
    }

    /** The label of the shaft the shuttle waits at or rides. */
    private static Component shaftLabel(ShuttleFleet fleet, Shuttle shuttle) {
        return shuttle.lift == null ? ServusShuttlesMachineKinds.ELEVATOR.inChat(Component.translatable(KEY + "lift"))
                : MachineLabels.chatAt(fleet.level(), shuttle.lift);
    }

    /** The floor the car carries the shuttle to. */
    private static int rideFloor(ShuttleFleet fleet, Shuttle shuttle) {
        LiftCar car = fleet.lift(shuttle.lift);
        int y = shuttle.motion.path().get(shuttle.rideEnd).y();
        return car == null ? 0 : StandReason.floor(y, car.bottomY());
    }
}
