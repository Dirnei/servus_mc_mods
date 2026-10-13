package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Which Ender Anchors this player shows the range of. Client only and never
 * sent to the server: off for every anchor whenever the game starts, and
 * forgotten on logout.
 */
@EventBusSubscriber(modid = ServusFoundationIds.MOD_ID, value = Dist.CLIENT)
public final class AnchorRangeView {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> SHOWN = new HashMap<>();

    private AnchorRangeView() {
    }

    public static boolean isShown(ResourceKey<Level> dimension, BlockPos pos) {
        Set<BlockPos> shown = SHOWN.get(dimension);
        return shown != null && shown.contains(pos);
    }

    /** Turns the range of the anchor at that position on or off. */
    public static void toggle(ResourceKey<Level> dimension, BlockPos pos) {
        Set<BlockPos> shown = SHOWN.computeIfAbsent(dimension, key -> new HashSet<>());
        if (!shown.remove(pos)) {
            shown.add(pos.immutable());
        }
    }

    /** The anchors shown in one dimension. */
    public static Set<BlockPos> shown(ResourceKey<Level> dimension) {
        return SHOWN.getOrDefault(dimension, Set.of());
    }

    /** Forgets an anchor that is gone. */
    public static void forget(ResourceKey<Level> dimension, BlockPos pos) {
        Set<BlockPos> shown = SHOWN.get(dimension);
        if (shown != null) {
            shown.remove(pos);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SHOWN.clear();
    }
}
