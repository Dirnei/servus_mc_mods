package io.github.dirnei.mc.servus.lib.data;

import io.github.dirnei.mc.servus.lib.graph.NodePos;

/** Events of a data network, drained once per tick. */
public sealed interface DataEvent {

    /** Something next to the cable may have changed: read its six sides again. */
    record SidesChanged(NodePos cable) implements DataEvent {
    }
}
