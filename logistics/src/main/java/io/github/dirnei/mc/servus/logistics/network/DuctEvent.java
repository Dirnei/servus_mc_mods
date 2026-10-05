package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.graph.NodePos;

/** Events posted to channel networks. */
public sealed interface DuctEvent {

    /** The duct at {@code pos} changed its side modes; rebuild its endpoints. */
    record ModesChanged(NodePos pos) implements DuctEvent {
    }
}
