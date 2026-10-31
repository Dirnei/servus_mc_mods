package io.github.dirnei.mc.servus.shuttles.locker;

import io.github.dirnei.mc.servus.shuttles.site.SiteChunks;
import java.util.Collection;

/**
 * Whether a player may order from a warehouse with a Handheld HH-1: standing
 * in the same dimension within the area of the site's chunks, widened on
 * every side by a margin in blocks. Pure Java.
 */
public final class OrderRange {

    private OrderRange() {
    }

    /**
     * @param siteChunks    the chunk keys of the site, as {@link SiteChunks} packs them
     * @param sameDimension whether the player stands in the site's dimension
     * @param blockX        the player's block column
     * @param blockZ        the player's block column
     * @param margin        how many blocks around the site still count
     */
    public static boolean inRange(Collection<Long> siteChunks, boolean sameDimension, int blockX, int blockZ, int margin) {
        if (!sameDimension) {
            return false;
        }
        for (long chunk : siteChunks) {
            int minX = SiteChunks.x(chunk) * 16 - margin;
            int minZ = SiteChunks.z(chunk) * 16 - margin;
            int maxX = SiteChunks.x(chunk) * 16 + 15 + margin;
            int maxZ = SiteChunks.z(chunk) * 16 + 15 + margin;
            if (blockX >= minX && blockX <= maxX && blockZ >= minZ && blockZ <= maxZ) {
                return true;
            }
        }
        return false;
    }
}
