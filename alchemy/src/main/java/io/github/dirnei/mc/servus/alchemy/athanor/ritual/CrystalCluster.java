package io.github.dirnei.mc.servus.alchemy.athanor.ritual;

import java.util.List;

/**
 * The cluster of crystal prisms that grows out of a vat: one tall spike in the middle and smaller ones leaning out around
 * it, each a six sided prism with a pointed tip. Sizes are in blocks from the cluster's root. Pure Java.
 */
public final class CrystalCluster {

    /**
     * One prism.
     *
     * @param height length from the root to the tip
     * @param radius of its six sided body
     * @param tilt   lean from upright, in radians
     * @param turn   direction it leans to, in radians around the vertical
     * @param start  when it starts to grow, in the cluster's own time units
     */
    public record Spike(float height, float radius, float tilt, float turn, float start) {
    }

    /** Where the prisms grow from, in blocks above the vat's floor: just under the surface of the full vat. */
    public static final float ROOT = 0.55F;
    /** The surface of the full vat, in blocks above its floor, below its rim at 0.75. */
    public static final float SURFACE = 0.625F;

    /** The share of a prism's length that is body; the rest is its tip. */
    public static final float BODY = 0.78F;
    /** The body widens toward the root by this factor. */
    public static final float ROOT_WIDENING = 1.08F;

    /** The prisms, the tallest first; it reaches the top of the Athanor's tanks from a root just under the vat's rim. */
    public static final List<Spike> SPIKES = List.of(
            new Spike(2.45F, 0.17F, 0.05F, 0.0F, 1.4F),
            new Spike(1.33F, 0.11F, 0.42F, 0.3F, 0.0F),
            new Spike(1.11F, 0.10F, 0.50F, 2.2F, 0.3F),
            new Spike(1.55F, 0.12F, 0.35F, 4.1F, 0.6F),
            new Spike(0.78F, 0.08F, 0.65F, 1.2F, 0.9F),
            new Spike(0.89F, 0.08F, 0.60F, 3.3F, 1.1F));

    /** The cluster's whole growth in its own time units: the last prism starts at 1.4 and grows for {@link #GROW}. */
    private static final float SPAN = 3.0F;
    private static final float GROW = 1.6F;

    private CrystalCluster() {
    }

    /** How large {@code spike} is when the cluster has grown {@code growth} of the way, 0 to a little over 1 while it overshoots. */
    public static float scale(Spike spike, float growth) {
        float own = (RitualTimeline.clamp01(growth) * SPAN - spike.start()) / GROW;
        return own <= 0 ? 0 : RitualTimeline.easeBack(own);
    }

    /** How high the tip of {@code spike} stands above the root at full size. */
    public static double top(Spike spike) {
        return spike.height() * Math.cos(spike.tilt());
    }
}
