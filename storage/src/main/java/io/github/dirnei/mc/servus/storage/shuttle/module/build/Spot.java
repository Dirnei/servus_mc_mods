package io.github.dirnei.mc.servus.storage.shuttle.module.build;

/** A point on a board, in pixels from its top left corner. Pure, no Minecraft imports. */
public record Spot(int x, int y) {

    /** Whether the point lies within the radius of this spot. */
    public boolean near(int px, int py, int radius) {
        int dx = px - x;
        int dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }
}
