package io.github.dirnei.mc.servus.delicacies.firebox;

/**
 * What makes a firebox hot. Pure Java. A firebox ticks its heat source once per server tick and answers
 * {@link Firebox#isHot()} from it, so a source that runs on FE instead of fuel can replace {@link FuelFire}
 * without the cooking spots noticing.
 */
public interface HeatSource {

    /** Advances one tick; {@code demanded} says whether some spot asked for heat since the last tick. */
    void tick(boolean demanded);

    /** Whether the spots cook right now. */
    boolean hot();

    /** How hot the source is, from 0 (cold) to 1 (full heat), for a gauge. */
    float heat();
}
