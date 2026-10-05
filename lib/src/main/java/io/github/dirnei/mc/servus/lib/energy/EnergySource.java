package io.github.dirnei.mc.servus.lib.energy;

/** Something energy can be taken from. */
@FunctionalInterface
public interface EnergySource {

    /**
     * @param max      most energy to take
     * @param simulate when true, nothing changes
     * @return energy taken (or that would be taken)
     */
    int extract(int max, boolean simulate);
}
