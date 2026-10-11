package io.github.dirnei.mc.servus.storage.client;

import io.github.dirnei.mc.servus.storage.field.FieldWorkerPayload;

/** The name plate of the shuttle working the field of the open Field Head, as the server last sent it. */
public final class ClientFieldWorker {

    private static volatile String name = "";

    private ClientFieldWorker() {
    }

    public static void update(FieldWorkerPayload payload) {
        name = payload.name();
    }

    /** The worker's plate, empty when no named shuttle works the field. */
    public static String name() {
        return name;
    }
}
