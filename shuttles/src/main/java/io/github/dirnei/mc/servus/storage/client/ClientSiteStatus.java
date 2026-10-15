package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.controller.SiteStatusPayload;

/** The last site status the server sent for the open controller GUI. */
public final class ClientSiteStatus {

    private static volatile SiteStatusPayload status = SiteStatusPayload.EMPTY;

    private ClientSiteStatus() {
    }

    public static void update(SiteStatusPayload payload) {
        status = payload;
    }

    public static void clear() {
        status = SiteStatusPayload.EMPTY;
    }

    public static SiteStatusPayload status() {
        return status;
    }
}
