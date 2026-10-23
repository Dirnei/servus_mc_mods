package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.shuttle.module.JobNeeds;
import net.minecraft.core.BlockPos;

/**
 * A fertilize run for a shuttle with a Spreader: pick fertilizer above the
 * field's reserve, dose the plots of the field the Field Head at that
 * position serves, and put the rest away.
 *
 * @param id   the job's id, unique in the world
 * @param head the Field Head of the field
 */
public record FertilizeJob(int id, BlockPos head) implements ShuttleJob {

    @Override
    public JobNeeds needs() {
        return JobNeeds.SPREADER;
    }
}
