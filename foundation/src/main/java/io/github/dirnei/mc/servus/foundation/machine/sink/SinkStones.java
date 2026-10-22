package io.github.dirnei.mc.servus.foundation.machine.sink;

import java.util.LinkedHashSet;
import java.util.List;

/** The stones of a Sink cube: a cube forms only when its blocks share one. Pure, no Minecraft imports. */
public final class SinkStones {

    private SinkStones() {
    }

    /** The distinct stone ids of {@code stones}, in the order they are first seen. */
    public static List<String> distinct(List<String> stones) {
        return List.copyOf(new LinkedHashSet<>(stones));
    }
}
