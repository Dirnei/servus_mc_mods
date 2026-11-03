package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.Spot;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** The spots this player switched a beam on for. Client memory only, gone when the player leaves the game. */
@EventBusSubscriber(modid = ServusNotesIds.MOD_ID, value = Dist.CLIENT)
public final class Beams {

    private static final Set<Spot> ON = new LinkedHashSet<>();

    private Beams() {
    }

    public static boolean isOn(Spot spot) {
        return ON.contains(spot.position());
    }

    /** Switches the beam at {@code spot} on, or off when it is on. */
    public static void toggle(Spot spot) {
        if (!ON.remove(spot.position())) {
            ON.add(spot.position());
        }
    }

    public static Set<Spot> all() {
        return Collections.unmodifiableSet(ON);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ON.clear();
    }
}
