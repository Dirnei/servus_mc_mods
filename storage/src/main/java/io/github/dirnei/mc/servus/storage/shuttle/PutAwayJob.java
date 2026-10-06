package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.core.BlockPos;

/**
 * One stack waiting in a dispatch station for a shuttle to carry it to its target.
 *
 * @param station the station whose buffer holds the stack
 * @param slot    the claimed buffer slot
 * @param key     what to carry
 * @param amount  how many at most
 * @param target  the storage block chosen for it
 */
public record PutAwayJob(BlockPos station, int slot, ItemKey key, int amount, BlockPos target) implements ShuttleJob {
}
