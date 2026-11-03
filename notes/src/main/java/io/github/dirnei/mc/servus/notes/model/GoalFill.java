package io.github.dirnei.mc.servus.notes.model;

/**
 * How a goal's bar fills for one viewer: the shared count first, then what the viewer carries on top of
 * it, both as parts of the target and together never more than the whole bar.
 */
public record GoalFill(float counted, float carried) {

    public static GoalFill of(Goal goal, int carriedItems) {
        return of(goal.count(), goal.target(), carriedItems);
    }

    public static GoalFill of(int count, int goalTarget, int carriedItems) {
        int target = Math.max(1, goalTarget);
        int counted = Math.clamp(count, 0, target);
        int carried = Math.clamp(carriedItems, 0, target - counted);
        return new GoalFill((float) counted / target, (float) carried / target);
    }
}
