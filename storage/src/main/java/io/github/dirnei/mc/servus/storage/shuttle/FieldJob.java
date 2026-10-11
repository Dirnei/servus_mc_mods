package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A field run for a shuttle with a Harvester: harvest, till and plant the
 * field the Field Head at that position serves, and put the harvest away.
 *
 * @param head the Field Head of the field
 */
public record FieldJob(BlockPos head) implements ShuttleJob {

    @Override
    public JobNeeds needs() {
        return JobNeeds.HARVESTER;
    }
}
