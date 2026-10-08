package io.github.dirnei.mc.servus.foundation.logistics.network;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;

/** Events posted to line networks. */
public sealed interface DuctEvent {

    /** The duct at {@code pos} changed modes, filters or priorities; rebuild its endpoints. */
    record ConfigChanged(NodePos pos) implements DuctEvent {
    }
}
