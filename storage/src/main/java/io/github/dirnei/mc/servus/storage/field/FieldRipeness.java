package io.github.dirnei.mc.servus.storage.field;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Which plots of a field hold a crop and which of those are ripe. Pure, no Minecraft imports. */
public final class FieldRipeness<T> {

    private final Set<T> planted = new HashSet<>();
    private final Set<T> ripe = new HashSet<>();

    /** Records what a plot holds now; a ripe plot is always planted. */
    public void update(T plot, boolean isPlanted, boolean isRipe) {
        if (isPlanted || isRipe) {
            planted.add(plot);
        } else {
            planted.remove(plot);
        }
        if (isRipe) {
            ripe.add(plot);
        } else {
            ripe.remove(plot);
        }
    }

    public void clear() {
        planted.clear();
        ripe.clear();
    }

    public int planted() {
        return planted.size();
    }

    public int ripe() {
        return ripe.size();
    }

    public boolean isRipe(T plot) {
        return ripe.contains(plot);
    }

    public Set<T> ripePlots() {
        return Collections.unmodifiableSet(ripe);
    }

    /** Ripe plots divided by planted plots, 0 when nothing is planted. */
    public double share() {
        return planted.isEmpty() ? 0 : (double) ripe.size() / planted.size();
    }

    /** Whether something is planted and the ripe share reaches the threshold. */
    public boolean due(double threshold) {
        return !planted.isEmpty() && share() >= threshold;
    }
}
