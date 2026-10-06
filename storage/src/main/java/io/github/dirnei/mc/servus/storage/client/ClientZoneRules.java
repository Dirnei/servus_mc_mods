package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.station.ZoneRulesSyncPayload;

/** The last zone overview the server sent for the open dispatch station. */
public final class ClientZoneRules {

    private static volatile ZoneRulesSyncPayload state = ZoneRulesSyncPayload.EMPTY;
    private static volatile int version;

    private ClientZoneRules() {
    }

    public static void update(ZoneRulesSyncPayload payload) {
        state = payload;
        version++;
    }

    public static void clear() {
        state = ZoneRulesSyncPayload.EMPTY;
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
