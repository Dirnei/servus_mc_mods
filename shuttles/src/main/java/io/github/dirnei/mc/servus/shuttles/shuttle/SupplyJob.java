package io.github.dirnei.mc.servus.shuttles.shuttle;

import net.minecraft.core.BlockPos;

/**
 * A supply run for a shuttle without a tool: pick what the Field Head at that
 * position is short of in its buffer from storage, riding lifts like a pick,
 * unload it at the head's rail and take stale items back.
 *
 * @param id   the job's id, unique in the world
 * @param head the Field Head whose buffer is refilled
 */
public record SupplyJob(int id, BlockPos head) implements ShuttleJob {
}
