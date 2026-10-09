package io.github.dirnei.mc.servus.foundation.data;

import java.util.Map;
import net.minecraft.core.BlockPos;

/** The duct load levels the server sent last; read by the goggles overlay. No client imports. */
public final class ClientDuctLoads {

    private static volatile Map<Long, Integer> levels = Map.of();

    private ClientDuctLoads() {
    }

    public static void update(DuctLoadPayload payload) {
        levels = Map.copyOf(payload.levels());
    }

    /** The load level of the duct at {@code pos}, 0 when idle or unknown. */
    public static int level(BlockPos pos) {
        return levels.getOrDefault(pos.asLong(), 0);
    }
}
