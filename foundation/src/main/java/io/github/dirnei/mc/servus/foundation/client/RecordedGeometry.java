package io.github.dirnei.mc.servus.foundation.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * A vertex consumer that records vertices with the pose already applied, then
 * submits them as one custom geometry. Drawing code written against a vertex
 * consumer draws into the recorder, then calls {@link #submit}.
 */
public final class RecordedGeometry implements VertexConsumer {

    private static final int FLOATS = 6;
    private static final int INTS = 4;

    /** Per vertex: x, y, z, u, v and line width; normals and the packed ints live in their own lists. */
    private final FloatArrayList floats = new FloatArrayList();
    private final FloatArrayList normals = new FloatArrayList();
    private final IntArrayList ints = new IntArrayList();
    private int vertices;

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        floats.add(x);
        floats.add(y);
        floats.add(z);
        floats.add(0);
        floats.add(0);
        floats.add(1);
        normals.add(0);
        normals.add(1);
        normals.add(0);
        ints.add(0xFFFFFFFF);
        ints.add(0);
        ints.add(0);
        ints.add(0);
        vertices++;
        return this;
    }

    private int floatAt() {
        return (vertices - 1) * FLOATS;
    }

    private int intAt() {
        return (vertices - 1) * INTS;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return setColor((a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF);
    }

    @Override
    public VertexConsumer setColor(int color) {
        ints.set(intAt(), color);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        floats.set(floatAt() + 3, u);
        floats.set(floatAt() + 4, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        ints.set(intAt() + 1, (v & 0xFFFF) << 16 | u & 0xFFFF);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        ints.set(intAt() + 2, (v & 0xFFFF) << 16 | u & 0xFFFF);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        int at = (vertices - 1) * 3;
        normals.set(at, x);
        normals.set(at + 1, y);
        normals.set(at + 2, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        floats.set(floatAt() + 5, width);
        return this;
    }

    private void replay(VertexConsumer consumer) {
        for (int i = 0; i < vertices; i++) {
            int f = i * FLOATS;
            int n = i * 3;
            int k = i * INTS;
            int overlay = ints.getInt(k + 1);
            int light = ints.getInt(k + 2);
            consumer.addVertex(floats.getFloat(f), floats.getFloat(f + 1), floats.getFloat(f + 2))
                    .setColor(ints.getInt(k))
                    .setUv(floats.getFloat(f + 3), floats.getFloat(f + 4))
                    .setUv1(overlay & 0xFFFF, overlay >>> 16)
                    .setUv2(light & 0xFFFF, light >>> 16)
                    .setNormal(normals.getFloat(n), normals.getFloat(n + 1), normals.getFloat(n + 2));
        }
    }

    /** Submits the recorded vertices; they already carry the pose, so they go in with an identity pose. */
    public void submit(SubmitNodeCollector collector, RenderType renderType) {
        if (vertices == 0) {
            return;
        }
        collector.submitCustomGeometry(new PoseStack(), renderType, (pose, consumer) -> replay(consumer));
    }
}
