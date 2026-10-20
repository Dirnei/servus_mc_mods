package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.logistics.network.FluidChannelNetwork.FluidKey;
import io.github.dirnei.mc.servus.shuttles.gantry.TankRef;
import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A tanker run: take fluid out of one gantry tank and bring it into another.
 * Fluid that does not fit there goes on to the next tank with room, and back
 * to the source when none has any.
 *
 * @param source     the master of the gantry the fluid comes from
 * @param sourceTank its tank, 0 left and 1 right
 * @param target     the master of the gantry the fluid goes to first
 * @param targetTank its tank
 * @param key        the fluid
 * @param amount     how much at most; a shuttle that carries less takes less
 */
public record FluidJob(BlockPos source, int sourceTank, BlockPos target, int targetTank, FluidKey key, int amount) implements ShuttleJob {

    @Override
    public JobNeeds needs() {
        return JobNeeds.TANKER;
    }

    public TankRef sourceRef() {
        return new TankRef(source, sourceTank);
    }

    public TankRef targetRef() {
        return new TankRef(target, targetTank);
    }
}
