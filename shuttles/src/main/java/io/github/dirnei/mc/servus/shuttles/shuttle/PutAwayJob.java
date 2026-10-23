package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;
import net.minecraft.core.BlockPos;

/**
 * One stack waiting in an inbound station for a shuttle to carry it to its target.
 *
 * @param id      the job's id, unique in the world
 * @param station the station whose buffer holds the stack
 * @param slot    the claimed buffer slot
 * @param key     what to carry
 * @param amount  how many at most
 * @param target  the storage block chosen for it
 */
public record PutAwayJob(int id, BlockPos station, int slot, ItemKey key, int amount, BlockPos target) implements ShuttleJob {
}
