package io.github.dirnei.mc.servus.foundation.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.neoforged.fml.ModList;

/** The mod id and display name of a namespace, for {@code @} search terms. */
final class ModNames {

    private static final Map<String, List<String>> BY_NAMESPACE = new ConcurrentHashMap<>();

    private ModNames() {
    }

    /** The namespace, plus the display name of the mod with that id when one is loaded. */
    static List<String> of(String namespace) {
        return BY_NAMESPACE.computeIfAbsent(namespace, ns -> ModList.get().getModContainerById(ns)
                .map(container -> List.of(ns, container.getModInfo().getDisplayName()))
                .orElse(List.of(ns)));
    }
}
