package io.github.dirnei.mc.servus.foundation.client;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * A buffer source for block entity renderers: each render type gets a
 * {@link RecordedGeometry}, and {@link #submit} hands them all to the
 * collector once the drawing is done. Items and text go straight to
 * {@link #collector()}.
 */
public final class SubmitBuffers {

    private final SubmitNodeCollector collector;
    private final Map<RenderType, RecordedGeometry> buffers = new LinkedHashMap<>();

    public SubmitBuffers(SubmitNodeCollector collector) {
        this.collector = collector;
    }

    public SubmitNodeCollector collector() {
        return collector;
    }

    /** The recorder for a render type; vertices keep the pose they were drawn with. */
    public RecordedGeometry getBuffer(RenderType renderType) {
        return buffers.computeIfAbsent(renderType, type -> new RecordedGeometry());
    }

    /** Submits everything drawn so far. */
    public void submit() {
        buffers.forEach((type, geometry) -> geometry.submit(collector, type));
        buffers.clear();
    }
}
