package io.github.dirnei.mc.servus.lib.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * What a graph operation did to the components.
 *
 * @param survivor component the operated position belongs to afterwards, or -1 when none
 * @param created  ids of components that did not exist before the operation
 * @param merged   ids of components that were absorbed into {@code survivor} and no longer exist
 * @param removed  ids of components that vanished because their last node was removed
 */
public record GraphChange(int survivor, List<Integer> created, List<Integer> merged, List<Integer> removed) {

    public static final int NO_COMPONENT = -1;

    public static final GraphChange NONE = new GraphChange(NO_COMPONENT, List.of(), List.of(), List.of());

    public GraphChange {
        created = List.copyOf(created);
        merged = List.copyOf(merged);
        removed = List.copyOf(removed);
    }

    public static GraphChange survivorOnly(int survivor) {
        return new GraphChange(survivor, List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
        return created.isEmpty() && merged.isEmpty() && removed.isEmpty();
    }

    /**
     * Combines this change with one that happened right after it. A component
     * that was created by the first change and merged or removed by the second
     * is dropped from both lists, because nobody outside ever needs to know.
     */
    public GraphChange andThen(GraphChange next) {
        List<Integer> created = new ArrayList<>(this.created);
        List<Integer> merged = new ArrayList<>(this.merged);
        List<Integer> removed = new ArrayList<>(this.removed);
        for (int id : next.created) {
            created.add(id);
        }
        for (int id : next.merged) {
            if (!created.remove(Integer.valueOf(id))) {
                merged.add(id);
            }
        }
        for (int id : next.removed) {
            if (!created.remove(Integer.valueOf(id))) {
                removed.add(id);
            }
        }
        return new GraphChange(next.survivor, created, merged, removed);
    }
}
