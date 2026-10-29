package io.github.dirnei.mc.servus.shuttles.rail;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.Optional;
import java.util.Set;

/**
 * Which side a newly placed block that docks to a rail turns its rail side
 * to: the rail the player clicked, else a rail beside it where the player
 * looks, else the only rail beside it. Empty when no rail decides it or the
 * player sneaks; the block then faces its front to the player. All sides are
 * horizontal. Pure Java.
 */
public final class RailFacing {

    private RailFacing() {
    }

    /**
     * @param clickedRail the side towards a rail whose side face the player clicked
     * @param railsBeside the sides with a rail next to the block
     * @param looking     the horizontal direction the player looks
     * @param sneaking    whether the player sneaks, which ignores every rail
     */
    public static Optional<Side> railSide(Optional<Side> clickedRail, Set<Side> railsBeside, Side looking, boolean sneaking) {
        if (sneaking) {
            return Optional.empty();
        }
        if (clickedRail.isPresent()) {
            return clickedRail;
        }
        if (railsBeside.contains(looking)) {
            return Optional.of(looking);
        }
        if (railsBeside.size() == 1) {
            return Optional.of(railsBeside.iterator().next());
        }
        return Optional.empty();
    }
}
