package io.github.dirnei.mc.servus.foundation.logistics.menu;

/** The last {@link CableChannelsPayload} the server sent for the open endpoint GUI. No client imports. */
public final class ClientCableChannels {

    private static volatile CableChannelsPayload last;

    private ClientCableChannels() {
    }

    public static void update(CableChannelsPayload payload) {
        last = payload;
    }

    /** The last payload; null before the first arrives. */
    public static CableChannelsPayload get() {
        return last;
    }

    /** Forgets the last payload, so a newly opened GUI does not show another side's data. */
    public static void clear() {
        last = null;
    }
}
