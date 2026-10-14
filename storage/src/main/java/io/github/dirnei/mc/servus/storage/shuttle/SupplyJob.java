package io.github.dirnei.mc.servus.storage.shuttle;

import net.minecraft.core.BlockPos;

/**
 * A supply run for a shuttle without a tool: pick what the Field Head at that
 * position is short of in its buffer from storage, riding lifts like a pick,
 * unload it at the head's rail and take stale items back.
 *
 * @param head the Field Head whose buffer is refilled
 */
public record SupplyJob(BlockPos head) implements ShuttleJob {
}
