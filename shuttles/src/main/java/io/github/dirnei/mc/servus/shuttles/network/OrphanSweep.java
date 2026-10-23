package io.github.dirnei.mc.servus.shuttles.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Finds claims whose owner is gone: a claim is an orphan once its owner was
 * unknown at two sweeps in a row, so an owner that is only being restored
 * gets a sweep's time to come back. Pure Java.
 *
 * @param <K> what identifies one claim
 */
public final class OrphanSweep<K> {

    private Set<K> suspects = new HashSet<>();

    /**
     * One sweep over every claim held right now.
     *
     * @param claims the claims held now
     * @param known  whether a claim's owner is known now
     * @return the orphans, to release; they are forgotten by the sweep
     */
    public List<K> sweep(Collection<K> claims, Predicate<K> known) {
        Set<K> unknown = new HashSet<>();
        List<K> orphans = new ArrayList<>();
        for (K claim : claims) {
            if (known.test(claim)) {
                continue;
            }
            if (suspects.contains(claim)) {
                orphans.add(claim);
            } else {
                unknown.add(claim);
            }
        }
        suspects = unknown;
        return orphans;
    }
}
