package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A field run for a shuttle with a Harvester: harvest, till and plant the
 * field the Field Head at that position serves, and put the harvest away.
 *
 * @param id   the job's id, unique in the world
 * @param head the Field Head of the field
 */
public record FieldJob(int id, BlockPos head) implements ShuttleJob {

    @Override
    public JobNeeds needs() {
        return JobNeeds.HARVESTER;
    }
}
