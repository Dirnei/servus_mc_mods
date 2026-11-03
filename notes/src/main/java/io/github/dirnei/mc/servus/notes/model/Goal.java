package io.github.dirnei.mc.servus.notes.model;

import java.util.Objects;
import java.util.UUID;

/**
 * An item goal of a task: how many of an item the task needs and how many are there, typed by hand.
 *
 * @param itemId the item's registry id as text, kept as is while the item does not exist
 * @param lastBy who set the count last, or who added the goal while nobody has
 */
public record Goal(UUID id, String itemId, int target, int count, Person lastBy) {

    public Goal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(lastBy, "lastBy");
    }

    /** Whether the count has reached the target. */
    public boolean reached() {
        return count >= target;
    }

    /** The count after adding {@code carried} items to it, never past the target and never below the count. */
    public int countWithCarried(int carried) {
        return count + Math.clamp(carried, 0, Math.max(0, target - count));
    }

    public Goal withItem(String newItemId) {
        return new Goal(id, newItemId, target, count, lastBy);
    }

    public Goal withTarget(int newTarget) {
        return new Goal(id, itemId, newTarget, count, lastBy);
    }

    /** This goal with a new count set by {@code by}. */
    public Goal withCount(int newCount, Person by) {
        return new Goal(id, itemId, target, newCount, by);
    }
}
