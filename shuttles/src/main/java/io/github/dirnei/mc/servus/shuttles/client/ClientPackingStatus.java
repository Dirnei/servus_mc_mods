package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.packing.PackingStatusPayload;

/** The last status line the server sent for the open Packing Station. */
public final class ClientPackingStatus {

    private static volatile PackingStatusPayload status = PackingStatusPayload.EMPTY;

    private ClientPackingStatus() {
    }

    public static void update(PackingStatusPayload payload) {
        status = payload;
    }

    public static void clear() {
        status = PackingStatusPayload.EMPTY;
    }

    public static PackingStatusPayload status() {
        return status;
    }
}
