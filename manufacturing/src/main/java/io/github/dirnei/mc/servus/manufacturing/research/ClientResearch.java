package io.github.dirnei.mc.servus.manufacturing.research;

import io.github.dirnei.mc.servus.manufacturing.network.ResearchSyncPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The client's view of its team's research, as last synced. Plain data so it
 * can live in common code; only the client writes it.
 */
public final class ClientResearch {

    private static volatile Set<ResourceLocation> completed = Set.of();
    private static volatile ResourceLocation current;
    private static volatile Map<String, Integer> progress = Map.of();

    private ClientResearch() {
    }

    public static void update(ResearchSyncPayload payload) {
        completed = new HashSet<>(payload.completed());
        current = payload.current().orElse(null);
        progress = new HashMap<>(payload.progressOfCurrent());
    }

    public static void clear() {
        completed = Set.of();
        current = null;
        progress = Map.of();
    }

    public static boolean isCompleted(ResourceLocation techId) {
        return completed.contains(techId);
    }

    public static Optional<ResourceLocation> current() {
        return Optional.ofNullable(current);
    }

    public static int consumed(String item) {
        return progress.getOrDefault(item, 0);
    }

    public static int totalConsumed() {
        return progress.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static boolean isAvailable(Techs.Entry entry) {
        if (completed.contains(entry.id())) {
            return false;
        }
        for (ResourceLocation parent : entry.definition().parents()) {
            if (!completed.contains(parent)) {
                return false;
            }
        }
        return true;
    }
}
