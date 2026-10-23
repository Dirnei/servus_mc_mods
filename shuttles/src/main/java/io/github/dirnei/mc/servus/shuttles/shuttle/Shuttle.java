package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.gantry.TankRef;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.ShuttleProfile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One shuttle of the fleet: the garage slot it lives in, what its modules let
 * it do, where it is and the jobs it carries. Every load shares the way of
 * the first, so one leg serves them all.
 */
final class Shuttle {

    private static int nextId;

    final Garage garage;
    final int slot;
    final int id = nextId++;
    ShuttleProfile profile;
    /** The shuttle's item as it sits in the garage, for drawing it. */
    ItemStack item = ItemStack.EMPTY;
    ShuttleMotion motion;
    Leg leg = Leg.HOME;
    final List<Load> loads = new ArrayList<>();
    /** Ticks left of the stop the shuttle stands in, 0 when it is not stopping. */
    int stop;
    /** The leg whose stop has started; its next arrival goes on without stopping again. */
    ShuttleMotion stopAt;
    /** The field run of a harvester out on a field job, else null. */
    HarvestRun run;
    /** The fertilize run of a Spreader out on a fertilize job, else null. */
    SpreadRun spread;
    /** The supply run of a shuttle out on a supply job, else null. */
    SupplyRun supply;
    /** Bottom of the shaft whose car the shuttle calls, rides or crosses; null when none. */
    BlockPos lift;
    /** Whether the shuttle crosses that shaft's deck rather than calling its car. */
    boolean crossing;
    /** Whether the shuttle has been on a segment of that shaft yet. */
    boolean inside;
    /** Whether the car carries the shuttle right now, towards path node rideEnd. */
    boolean riding;
    int rideEnd;
    /** Blocks driven last tick on the path speedFor; a new path starts from standing. */
    double speed;
    ShuttleMotion speedFor;
    /** Ticks left of the wait inside the garage while its door opens. */
    int door;
    /** The path whose door wait has started; its shuttle is shown waiting inside. */
    ShuttleMotion doorFor;
    /** Ticks left of the pause at a lift handover. */
    int handover;
    /** Whether that pause is over, so the waiting step may go on. */
    boolean handedOver;
    /** The path whose segments up to paidThrough are paid for. */
    ShuttleMotion paidFor;
    int paidThrough = -1;
    /** The gantry tank whose spout pumps into or out of the shuttle during its stop, else null. */
    TankRef pumping;
    /** Whether that spout pumps into the gantry rather than into the shuttle. */
    boolean pumpingIntoGantry;
    /** The wait of a shuttle restored from a save until its path's chunks have loaded; null once it goes on. */
    Settle restoring;
    /** Why a shuttle parked in its garage with cargo found no rack, or null while it is not parked so. */
    StrandReason strandReason;
    /** The direction of the last block the shuttle drove, {@link ShuttleTraffic#NONE} before its first. */
    int lastHeading = ShuttleTraffic.NONE;
    /** Whether the shuttle stands inside its garage; it leaves when it first moves on its path. */
    boolean inGarage = true;
    /** Blocks the shuttle may drive this tick before it is the follow gap behind its leader. */
    double room = Double.POSITIVE_INFINITY;
    /** The id of the shuttle it follows this tick, or -1, and how fast that one drove last tick. */
    int leader = -1;
    double leaderSpeed;
    /** The speed of the ring of followers it moves in this tick, NaN when it is in none. */
    double ringSpeed = Double.NaN;
    /** Its following as clients last got it, to sync when it changes. */
    long followShown;

    Shuttle(Garage garage, int slot, ShuttleProfile profile) {
        this.garage = garage;
        this.slot = slot;
        this.profile = profile;
    }

    boolean busy() {
        return !loads.isEmpty();
    }

    /** Out of the garage: on a job or still driving home. */
    boolean out() {
        return busy() || (motion != null && !motion.arrived());
    }

    /** Whether the shuttle is inside its garage: parked there, or given a job and not moved yet. */
    boolean insideGarage() {
        return inGarage && motion != null && motion.segment() == 0 && motion.progress() == 0;
    }

    /** Whether the shuttle stands inside its garage at the start of a path out through the doorway. */
    boolean leavingGarage() {
        return insideGarage() && !motion.arrived() && motion.start().equals(garage.inside);
    }

    /** Whether the door wait of the shuttle's path has started, so it is shown inside facing out. */
    boolean atOpenDoor() {
        return doorFor == motion && leavingGarage();
    }

    /** Whether the shuttle drives in a ring of followers this tick. */
    boolean inRing() {
        return !Double.isNaN(ringSpeed);
    }

    ShuttleJob lead() {
        return loads.isEmpty() ? null : loads.get(0).job;
    }

    /** The node the shuttle stands on or last passed. */
    NodePos at() {
        return motion.currentNode();
    }

    /** The field run of any kind the shuttle is out on, else null. */
    FieldRun fieldRun() {
        if (run != null) {
            return run;
        }
        return spread != null ? spread : supply;
    }

    /** The cargo drawn on the cart: the first load that holds something. */
    ItemStack shownCargo() {
        FieldRun fieldRun = fieldRun();
        if (fieldRun != null) {
            return fieldRun.shown();
        }
        for (Load load : loads) {
            if (!load.cargo.isEmpty()) {
                return load.cargo.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** The fluid the first load that holds some carries, empty when none does. */
    FluidStack shownFluid() {
        for (Load load : loads) {
            if (!load.fluid.isEmpty()) {
                return load.fluid;
            }
        }
        return FluidStack.EMPTY;
    }

    /** One job a shuttle carries out and what it holds for it. */
    static final class Load {
        final ShuttleJob job;
        ItemStack cargo = ItemStack.EMPTY;
        /** The fluid a tanker run carries. */
        FluidStack fluid = FluidStack.EMPTY;
        /** The tank a tanker run's fluid goes to next; starts as the job's target. */
        TankRef fluidTarget;
        /** Tanks the fluid was offered to on this run. */
        final Set<TankRef> tried = new HashSet<>();
        /** The racks whose slots are locked for this load's cargo, in the order the shuttle visits them. */
        final List<BlockPos> stops = new ArrayList<>();
        /** Whether the cargo goes into storage: a loaded put-away, or a pick whose destination did not take it. */
        boolean storing;

        Load(ShuttleJob job) {
            this.job = job;
            if (job instanceof FluidJob fluidJob) {
                fluidTarget = fluidJob.targetRef();
            }
            if (job instanceof PutAwayJob putAway) {
                stops.add(putAway.target());
            }
        }
    }
}
