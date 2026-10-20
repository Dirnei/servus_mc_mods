package io.github.dirnei.mc.servus.delicacies.firebox;

/**
 * A firebox as the cooking spots on top of it see it. Pure Java. Blocks offer it through
 * {@link DelicaciesCapabilities#FIREBOX}; a spot asks the block below it.
 */
public interface Firebox {

    /** Whether the firebox heats its spots right now. */
    boolean isHot();

    /** Tells the firebox that a spot has something to cook this tick, so it lights its fuel when it is out. */
    void demandHeat();
}
