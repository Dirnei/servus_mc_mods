package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.station.StationStatePayload;

/** The last state the server sent for the open dispatch station. */
public final class ClientStationState {

    private static volatile StationStatePayload state = StationStatePayload.EMPTY;

    private ClientStationState() {
    }

    public static void update(StationStatePayload payload) {
        state = payload;
    }

    public static void clear() {
        state = StationStatePayload.EMPTY;
    }

    public static StationStatePayload state() {
        return state;
    }
}
