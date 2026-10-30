package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidRole;
import io.github.dirnei.mc.servus.shuttles.fluid.FluidStoreRef;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A tanker run: take fluid out of one fluid store and bring it into another.
 * Fluid that does not fit there goes on to the next fluid store with room; a
 * put-away brings what is still left back to its source, a restock loses it.
 *
 * @param id         the job's id, unique in the world
 * @param source     the port bay of a Fluid Rack shelf or the lower half of a fluid station
 * @param sourceTank its tank: the tank index on a fluid station, or {@link FluidStoreRef#RACK}
 * @param target     the port bay of the shelf or the lower half of the station the fluid goes to first
 * @param targetTank its tank
 * @param key        the fluid
 * @param amount     how much at most; a shuttle that carries less takes less
 * @param purpose    whether it empties a put-away source or fills a kept Export tank
 */
public record FluidJob(int id, BlockPos source, int sourceTank, BlockPos target, int targetTank, FluidKey key, int amount,
                       Purpose purpose) implements ShuttleJob {

    /** What a tanker run is for. */
    public enum Purpose {
        /** From an Import tank or a Fluid Inbound tank into fluid stores. */
        PUT_AWAY,
        /** From a Storage tank into a kept Export tank. */
        RESTOCK
    }

    @Override
    public JobNeeds needs() {
        return JobNeeds.TANKER;
    }

    public FluidStoreRef sourceRef() {
        return new FluidStoreRef(source, sourceTank);
    }

    public FluidStoreRef targetRef() {
        return new FluidStoreRef(target, targetTank);
    }

    /** The role the source must still have for the tanker to take fluid there. */
    public FluidRole sourceRole() {
        return purpose == Purpose.RESTOCK ? FluidRole.STORAGE : FluidRole.IMPORT;
    }

    /** The role a store the fluid goes to must have: Export for a restock's own target, else Storage. */
    public FluidRole targetRole(FluidStoreRef store) {
        return purpose == Purpose.RESTOCK && store.equals(targetRef()) ? FluidRole.EXPORT : FluidRole.STORAGE;
    }

    /** Whether fluid that no store takes goes back to the source. */
    public boolean returnsToSource() {
        return purpose == Purpose.PUT_AWAY;
    }
}
