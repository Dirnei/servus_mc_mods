package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** The last zone list and rule the server sent for the open dispatch station. */
public final class ClientZoneRules {

    private static volatile ZoneRulesSyncPayload state =
            new ZoneRulesSyncPayload(false, "", List.of(), "", "", false, "", ItemStack.EMPTY, "", true);
    private static volatile int version;

    private ClientZoneRules() {
    }

    public static void update(ZoneRulesSyncPayload payload) {
        state = payload;
        version++;
    }

    public static void clear() {
        state = new ZoneRulesSyncPayload(false, "", List.of(), "", "", false, "", ItemStack.EMPTY, "", true);
        version++;
    }

    public static ZoneRulesSyncPayload state() {
        return state;
    }

    /** Bumps on every update so the screen can react lazily. */
    public static int version() {
        return version;
    }
}
