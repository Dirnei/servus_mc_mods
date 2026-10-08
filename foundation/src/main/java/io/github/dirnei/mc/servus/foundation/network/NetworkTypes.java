package io.github.dirnei.mc.servus.foundation.network;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry of network types. Iteration order is registration order, which is
 * also the order in which types tick.
 */
public final class NetworkTypes {

    private static final Map<ResourceLocation, NetworkType<?, ?, ?>> TYPES = new LinkedHashMap<>();

    private NetworkTypes() {
    }

    public static synchronized <T extends NetworkType<?, ?, ?>> T register(T type) {
        NetworkType<?, ?, ?> previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null) {
            throw new IllegalStateException("Network type already registered: " + type.id());
        }
        return type;
    }

    public static synchronized Collection<NetworkType<?, ?, ?>> all() {
        return Collections.unmodifiableCollection(TYPES.values());
    }
}
