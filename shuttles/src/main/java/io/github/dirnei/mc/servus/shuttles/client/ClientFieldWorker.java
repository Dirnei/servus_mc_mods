package io.github.dirnei.mc.servus.shuttles.client;

import io.github.dirnei.mc.servus.shuttles.field.FieldWorkerPayload;

/** The name plates of the shuttles working the field of the open Field Head, as the server last sent them. */
public final class ClientFieldWorker {

    private static volatile String name = "";
    private static volatile String spreader = "";

    private ClientFieldWorker() {
    }

    public static void update(FieldWorkerPayload payload) {
        name = payload.name();
        spreader = payload.spreader();
    }

    /** The harvester's plate, empty when no named shuttle harvests the field. */
    public static String name() {
        return name;
    }

    /** The Spreader's plate, empty when no named Spreader doses the field. */
    public static String spreader() {
        return spreader;
    }
}
