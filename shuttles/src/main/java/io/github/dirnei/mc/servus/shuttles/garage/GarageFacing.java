package io.github.dirnei.mc.servus.shuttles.garage;

import io.github.dirnei.mc.servus.foundation.graph.Side;
import java.util.Optional;
import java.util.Set;

/**
 * Which side a newly placed garage turns its doorway to: the rail the player
 * clicked, else a rail beside it where the player looks, else the only rail
 * beside it, else where the player looks, so the front faces the player.
 * All sides are horizontal. Pure Java.
 */
public final class GarageFacing {

    private GarageFacing() {
    }

    /**
     * @param clickedRail the side towards a rail whose side face the player clicked
     * @param railsBeside the sides with a warehouse rail next to the garage
     * @param looking     the horizontal direction the player looks
     */
    public static Side doorway(Optional<Side> clickedRail, Set<Side> railsBeside, Side looking) {
        if (clickedRail.isPresent()) {
            return clickedRail.get();
        }
        if (railsBeside.contains(looking)) {
            return looking;
        }
        if (railsBeside.size() == 1) {
            return railsBeside.iterator().next();
        }
        return looking;
    }
}
