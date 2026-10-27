package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.gantry.GantryMode;
import io.github.dirnei.mc.servus.shuttles.gantry.FluidStoreRef;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A tanker run: take fluid out of one fluid store and bring it into another.
 * Fluid that does not fit there goes on to the next fluid store with room; a
 * put-away brings what is still left back to its source, a restock loses it.
 *
 * @param id         the job's id, unique in the world
 * @param source     the master of the gantry the fluid comes from, or the port bay of a Fluid Rack section
 * @param sourceTank its tank, 0 left and 1 right, or {@link FluidStoreRef#RACK}
 * @param target     the master of the gantry or the port bay of the section the fluid goes to first
 * @param targetTank its tank
 * @param key        the fluid
 * @param amount     how much at most; a shuttle that carries less takes less
 * @param purpose    whether it empties an Import tank or fills a kept Export tank
 */
public record FluidJob(int id, BlockPos source, int sourceTank, BlockPos target, int targetTank, FluidKey key, int amount,
                       Purpose purpose) implements ShuttleJob {

    /** What a tanker run is for. */
    public enum Purpose {
        /** From an Import tank into Storage tanks. */
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

    /** The mode the source must still have for the tanker to take fluid there; a Fluid Rack section counts as Storage. */
    public GantryMode sourceMode() {
        return purpose == Purpose.RESTOCK ? GantryMode.STORAGE : GantryMode.IMPORT;
    }

    /** The mode a store the fluid goes to must have: Export for a restock's own target, else Storage. */
    public GantryMode targetMode(FluidStoreRef store) {
        return purpose == Purpose.RESTOCK && store.equals(targetRef()) ? GantryMode.EXPORT : GantryMode.STORAGE;
    }

    /** Whether fluid that no store takes goes back to the source. */
    public boolean returnsToSource() {
        return purpose == Purpose.PUT_AWAY;
    }
}
