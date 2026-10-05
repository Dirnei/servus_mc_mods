package io.github.dirnei.mc.servus.logistics.network;

import io.github.dirnei.mc.servus.lib.graph.NodePos;

/** Events posted to channel networks. */
public sealed interface DuctEvent {

    /** The duct at {@code pos} changed modes, filters or priorities; rebuild its endpoints. */
    record ConfigChanged(NodePos pos) implements DuctEvent {
    }
}
