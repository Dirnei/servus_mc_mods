package io.github.dirnei.mc.servus.alchemy.hollow;

import io.github.dirnei.mc.servus.alchemy.hollow.VeinSyncPayload.ShownVein;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/** The veins the server sent this client, for the names and colours of Seeds. Empty on a dedicated server. */
public final class ClientVeins {

    private static volatile Map<Identifier, ShownVein> veins = Map.of();

    private ClientVeins() {
    }

    public static void update(VeinSyncPayload payload) {
        Map<Identifier, ShownVein> received = new LinkedHashMap<>();
        payload.veins().forEach(vein -> received.put(vein.id(), vein));
        veins = Map.copyOf(received);
    }

    public static Optional<ShownVein> get(Identifier id) {
        return Optional.ofNullable(veins.get(id));
    }

    /** The ids of the veins, in the order the server sent them. */
    public static List<Identifier> ids() {
        return veins.values().stream().map(ShownVein::id).toList();
    }
}
