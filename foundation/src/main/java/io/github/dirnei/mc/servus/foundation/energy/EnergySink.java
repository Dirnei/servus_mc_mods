package io.github.dirnei.mc.servus.foundation.energy;

/** Something energy can be given to. */
@FunctionalInterface
public interface EnergySink {

    /**
     * @param max      most energy offered
     * @param simulate when true, nothing changes
     * @return energy accepted (or that would be accepted)
     */
    int receive(int max, boolean simulate);
}
