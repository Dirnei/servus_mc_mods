package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.shuttle.ShuttleSyncPayload;
import io.github.dirnei.mc.servus.storage.shuttle.ShuttleView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Client side: the last known shuttles per garage and when they arrived. */
public final class ClientShuttles {

    /** @param views the shuttles as sent; @param receivedTick client game time at receipt */
    public record Fleet(List<ShuttleView> views, long receivedTick) {
    }

    private static final Map<BlockPos, Fleet> FLEETS = new HashMap<>();

    private ClientShuttles() {
    }

    public static void update(ShuttleSyncPayload payload) {
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        FLEETS.put(payload.garage(), new Fleet(payload.shuttles(), now));
    }

    public static Fleet fleet(BlockPos garage) {
        return FLEETS.get(garage);
    }

    public static void forget(BlockPos garage) {
        FLEETS.remove(garage);
    }

    public static void clear() {
        FLEETS.clear();
    }
}
