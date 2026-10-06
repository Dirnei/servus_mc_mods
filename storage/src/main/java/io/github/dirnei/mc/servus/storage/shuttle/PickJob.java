package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.core.BlockPos;

/**
 * One load of a terminal request: up to a stack from one source, delivered
 * into the output slots of the terminal the request was made at.
 *
 * @param requestId the request this load belongs to
 * @param key       what to fetch
 * @param amount    how many at most, never more than a stack
 * @param source    the storage block to take it from
 * @param terminal  where to deliver
 */
public record PickJob(int requestId, ItemKey key, int amount, BlockPos source, BlockPos terminal) implements ShuttleJob {
}
