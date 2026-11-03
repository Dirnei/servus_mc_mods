package io.github.dirnei.mc.servus.notes.client;

import io.github.dirnei.mc.servus.notes.ServusNotesIds;
import io.github.dirnei.mc.servus.notes.model.ListView;
import io.github.dirnei.mc.servus.notes.model.Task;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * The notebook's motion, client only: a ticked check mark pops in with a few sparks and the strike
 * runs across the text; a page whose content changes turns in from the spine.
 */
final class NotebookMotion {

    static final Identifier CHECK = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/check");
    private static final Identifier SPARK = Identifier.fromNamespaceAndPath(ServusNotesIds.MOD_ID, "notebook/spark");
    static final int CHECK_SIZE = 9;
    private static final int SPARK_SIZE = 3;
    private static final int SPARKS = 6;
    private static final float SPARK_REACH = 11F;

    private static final long POP_MILLIS = 320;
    private static final long SPARK_MILLIS = 450;
    private static final long FLIP_MILLIS = 200;
    /** How long a tick sent by this client waits for the server's snapshot before it pops without motion. */
    private static final long PENDING_MILLIS = 3000;
    private static final float FLIP_FROM = 0.8F;

    private final Map<UUID, Long> pending = new HashMap<>();
    private final Map<UUID, Long> pops = new HashMap<>();
    private long leftFlipStart = Long.MIN_VALUE;
    private long rightFlipStart = Long.MIN_VALUE;

    /** This client just asked to tick the task; it pops once the server's snapshot shows it done. */
    void tickSent(UUID taskId) {
        pending.put(taskId, Util.getMillis());
    }

    /** Starts the pops of pending ticks that {@code after} shows done. */
    void listChanged(@Nullable ListView after) {
        if (after == null) {
            return;
        }
        long now = Util.getMillis();
        for (Iterator<Map.Entry<UUID, Long>> it = pending.entrySet().iterator(); it.hasNext();) {
            Map.Entry<UUID, Long> entry = it.next();
            boolean done = after.list().task(entry.getKey()).map(Task::isDone).orElse(false);
            if (done) {
                pops.put(entry.getKey(), now);
                it.remove();
            } else if (now - entry.getValue() > PENDING_MILLIS) {
                it.remove();
            }
        }
    }

    /** Turns both pages in, as when a project opens or closes. */
    void flipBoth() {
        leftFlipStart = Util.getMillis();
        rightFlipStart = leftFlipStart;
    }

    /** Turns the right page in, as when another task opens. */
    void flipRight() {
        rightFlipStart = Util.getMillis();
    }

    /** The horizontal scale of the left page while it turns in, 1 when it lies flat. */
    float leftFlipScale() {
        return flipScale(leftFlipStart);
    }

    /** The horizontal scale of the right page while it turns in, 1 when it lies flat. */
    float rightFlipScale() {
        return flipScale(rightFlipStart);
    }

    boolean flipping() {
        return leftFlipScale() < 1F || rightFlipScale() < 1F;
    }

    private static float flipScale(long start) {
        return FLIP_FROM + (1F - FLIP_FROM) * easeOut(progress(start, FLIP_MILLIS));
    }

    /** How far the strike has run across a done task's text, 0 to 1. */
    float strike(UUID taskId) {
        Long start = pops.get(taskId);
        return start == null ? 1F : progress(start, POP_MILLIS);
    }

    /** Draws the check mark of a done task whose box has its top left at {@code x, y}, popping and sparking after a fresh tick. */
    void extractCheck(GuiGraphicsExtractor graphics, UUID taskId, int x, int y) {
        Long start = pops.get(taskId);
        float centreX = x + 4.5F;
        float centreY = y + 2.5F;
        float scale = 1F;
        if (start != null) {
            long elapsed = Util.getMillis() - start;
            if (elapsed > SPARK_MILLIS) {
                pops.remove(taskId);
            } else {
                scale = popScale(progress(start, POP_MILLIS));
                extractSparks(graphics, centreX, centreY + 2, progress(start, SPARK_MILLIS));
            }
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(centreX, centreY);
        graphics.pose().scale(scale, scale);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CHECK, -CHECK_SIZE / 2, -CHECK_SIZE / 2, CHECK_SIZE, CHECK_SIZE);
        graphics.pose().popMatrix();
    }

    private static void extractSparks(GuiGraphicsExtractor graphics, float x, float y, float t) {
        if (t >= 1F) {
            return;
        }
        float reach = 2F + SPARK_REACH * easeOut(t);
        int alpha = Math.round(255 * (1F - t));
        for (int i = 0; i < SPARKS; i++) {
            double angle = Math.toRadians(i * 360.0 / SPARKS);
            int sx = Math.round(x + (float) Math.cos(angle) * reach) - SPARK_SIZE / 2;
            int sy = Math.round(y + (float) Math.sin(angle) * reach) - SPARK_SIZE / 2;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPARK, sx, sy, SPARK_SIZE, SPARK_SIZE, (alpha << 24) | 0xFFFFFF);
        }
    }

    /** Grows from 0 past 1.5 and settles at 1. */
    private static float popScale(float t) {
        return t < 0.6F ? 1.5F * t / 0.6F : 1.5F - 0.5F * (t - 0.6F) / 0.4F;
    }

    private static float easeOut(float t) {
        return 1F - (1F - t) * (1F - t);
    }

    private static float progress(long start, long duration) {
        if (start == Long.MIN_VALUE) {
            return 1F;
        }
        return Math.min(1F, (Util.getMillis() - start) / (float) duration);
    }
}
