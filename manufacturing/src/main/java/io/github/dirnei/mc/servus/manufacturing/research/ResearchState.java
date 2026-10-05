package io.github.dirnei.mc.servus.manufacturing.research;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Research progress of one team. Pure Java, no Minecraft types.
 */
public final class ResearchState {

    private final Set<String> completed = new LinkedHashSet<>();
    private String current;
    private final Map<String, Map<String, Integer>> progress = new HashMap<>();

    public boolean isCompleted(String techId) {
        return completed.contains(techId);
    }

    public Set<String> completed() {
        return Collections.unmodifiableSet(completed);
    }

    public Optional<String> current() {
        return Optional.ofNullable(current);
    }

    /** A tech is available when it is not completed and all parents are. */
    public boolean isAvailable(TechNode tech) {
        if (completed.contains(tech.id())) {
            return false;
        }
        for (String parent : tech.parents()) {
            if (!completed.contains(parent)) {
                return false;
            }
        }
        return true;
    }

    /** Makes the tech the current research. Refused when not available. */
    public boolean start(TechNode tech) {
        if (!isAvailable(tech)) {
            return false;
        }
        current = tech.id();
        return true;
    }

    public void clearCurrent() {
        current = null;
    }

    public int consumed(String techId, String item) {
        Map<String, Integer> counts = progress.get(techId);
        return counts == null ? 0 : counts.getOrDefault(item, 0);
    }

    /** Packs of {@code item} the current research still needs; 0 when the tech is not current. */
    public int remaining(TechNode tech, String item) {
        if (!tech.id().equals(current)) {
            return 0;
        }
        return Math.max(0, tech.cost().getOrDefault(item, 0) - consumed(tech.id(), item));
    }

    public int totalConsumed(String techId) {
        Map<String, Integer> counts = progress.get(techId);
        return counts == null ? 0 : counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Records one consumed pack for the current research.
     *
     * @return true when the tech just completed
     * @throws IllegalStateException when the pack is not needed
     */
    public boolean consume(TechNode tech, String item) {
        if (remaining(tech, item) <= 0) {
            throw new IllegalStateException("Research " + tech.id() + " does not need " + item);
        }
        progress.computeIfAbsent(tech.id(), id -> new HashMap<>()).merge(item, 1, Integer::sum);
        for (String costItem : tech.cost().keySet()) {
            if (remaining(tech, costItem) > 0) {
                return false;
            }
        }
        completed.add(tech.id());
        progress.remove(tech.id());
        current = null;
        return true;
    }

    // Persistence helpers

    public Map<String, Map<String, Integer>> progress() {
        return Collections.unmodifiableMap(progress);
    }

    public void restore(Set<String> completedIds, String currentId, Map<String, Map<String, Integer>> progressMap) {
        completed.clear();
        completed.addAll(completedIds);
        current = currentId;
        progress.clear();
        progressMap.forEach((tech, counts) -> progress.put(tech, new HashMap<>(counts)));
    }
}
