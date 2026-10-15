package io.github.dirnei.mc.servus.shuttles.field;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Which plots of a field hold a crop, which of those are ripe, and which are
 * open for field work: to till or to plant. Pure Java.
 */
public final class FieldRipeness<T> {

    private final Set<T> planted = new HashSet<>();
    private final Set<T> ripe = new HashSet<>();
    private final Set<T> tillable = new HashSet<>();
    private final Set<T> plantable = new HashSet<>();
    private final Map<T, PlotState> states = new HashMap<>();

    /** Records what a plot holds now; a ripe plot is always planted. */
    public void update(T plot, boolean isPlanted, boolean isRipe) {
        update(plot, PlotState.of(isPlanted, isRipe));
    }

    /** Records the state of a plot now. */
    public void update(T plot, PlotState state) {
        boolean isPlanted = state == PlotState.GROWING || state == PlotState.RIPE;
        set(planted, plot, isPlanted);
        set(ripe, plot, state == PlotState.RIPE);
        set(tillable, plot, state == PlotState.TILLABLE);
        set(plantable, plot, state == PlotState.PLANTABLE);
        if (state == PlotState.BLOCKED) {
            states.remove(plot);
        } else {
            states.put(plot, state);
        }
    }

    private static <T> void set(Set<T> set, T plot, boolean member) {
        if (member) {
            set.add(plot);
        } else {
            set.remove(plot);
        }
    }

    public void clear() {
        planted.clear();
        ripe.clear();
        tillable.clear();
        plantable.clear();
        states.clear();
    }

    /** The recorded state of a plot; blocked when nothing was recorded. */
    public PlotState state(T plot) {
        return states.getOrDefault(plot, PlotState.BLOCKED);
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

    /** Planted plots whose crop is not ripe yet. */
    public Set<T> growing() {
        Set<T> growing = new HashSet<>(planted);
        growing.removeAll(ripe);
        return growing;
    }

    /** Empty plots on dirt or grass. */
    public int tillable() {
        return tillable.size();
    }

    /** Empty plots on farmland. */
    public int plantable() {
        return plantable.size();
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
