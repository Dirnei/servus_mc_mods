package io.github.dirnei.mc.servus.storage.shuttle;

import io.github.dirnei.mc.servus.storage.network.ItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/**
 * One pull waiting for or riding on a shuttle.
 *
 * @param key       what to fetch
 * @param amount    how many at most, never more than a stack
 * @param requester the player who asked; delivery goes to their inventory
 * @param terminal  where the pull was made; leftovers drop here
 */
public record PickJob(ItemKey key, int amount, Player requester, BlockPos terminal) {
}
