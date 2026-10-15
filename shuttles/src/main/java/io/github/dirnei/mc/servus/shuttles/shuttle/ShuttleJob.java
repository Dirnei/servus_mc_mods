package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;

/** Something a shuttle does: fetch a pull, carry a put-away, work a field or supply a Field Head. All share one queue. */
public sealed interface ShuttleJob permits PickJob, PutAwayJob, FieldJob, FertilizeJob, SupplyJob {

    /** The modules a shuttle needs for this job; warehouse jobs need none and refuse field only shuttles. */
    default JobNeeds needs() {
        return JobNeeds.WAREHOUSE;
    }

    /** Whether the other job takes the same way, so a shuttle with room can carry both on one trip. */
    default boolean sameWay(ShuttleJob other) {
        if (this instanceof PickJob pick && other instanceof PickJob that) {
            return pick.source().equals(that.source()) && pick.terminal().equals(that.terminal());
        }
        if (this instanceof PutAwayJob putAway && other instanceof PutAwayJob that) {
            return putAway.station().equals(that.station()) && putAway.target().equals(that.target());
        }
        return false;
    }
}
