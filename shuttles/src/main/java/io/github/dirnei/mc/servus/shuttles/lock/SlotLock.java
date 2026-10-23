package io.github.dirnei.mc.servus.shuttles.lock;

import io.github.dirnei.mc.servus.shuttles.network.ItemKey;

/**
 * One slot reserved for one shuttle job.
 *
 * @param kind   whether the job fills the slot (IN) or empties it (OUT)
 * @param key    the item it brings or takes
 * @param amount how many
 * @param jobId  the job that holds the lock
 */
public record SlotLock(Kind kind, ItemKey key, int amount, int jobId) {

    /** IN for a slot the job fills, OUT for a slot it empties. */
    public enum Kind { IN, OUT }
}
