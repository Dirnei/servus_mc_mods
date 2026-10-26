package io.github.dirnei.mc.servus.foundation.client;

import io.github.dirnei.mc.servus.foundation.ui.ListScroll;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The family's vertical scrollbar: a sunken track beside a list or grid and
 * a raised thumb as long as the visible share of the content. The thumb can
 * be grabbed and dragged, a press on the track centres the thumb there and
 * keeps dragging, and while nothing scrolls a greyed thumb fills the track.
 * The bar owns the scroll offset; content, visible size and offset share one
 * unit (rows, lines or pixels).
 *
 * <p>Use: {@link #setArea} with the absolute track (the frame draws 1 pixel
 * around it), {@link #setContent} whenever the content changes, read
 * {@link #offset} when drawing and hit testing, call {@link #render}, and
 * forward the mouse: ask {@link #mouseClicked} first and stop when it took
 * the press, return early from the screen's drag and release while
 * {@link #mouseDragged} and {@link #mouseReleased} say they held the drag,
 * and call {@link #scrollBy} with the view's own wheel step.
 */
public final class Scrollbar {

    /** Track width in panels. */
    public static final int WIDE = 8;
    /** Track width in popups and small lists. */
    public static final int NARROW = 4;

    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF373737;
    private static final int TRACK = 0xFF8B8B8B;
    private static final int FACE = 0xFFC6C6C6;
    private static final int FACE_HOVERED = 0xFFE0E0E0;
    private static final int SHADOW = 0xFF555555;
    private static final int GREYED = 0xFF9A9A9A;

    private int x;
    private int y;
    private int width;
    private int height;
    private int total;
    private int visible;
    private int offset;
    private boolean dragging;
    private double grab;

    /** The track, absolute, without its 1 pixel frame. */
    public void setArea(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** The size of the content and of the part on show; the offset stays where it still fits. */
    public void setContent(int total, int visible) {
        this.total = total;
        this.visible = visible;
        offset = ListScroll.clamp(offset, total, visible);
    }

    public int offset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = ListScroll.clamp(offset, total, visible);
    }

    /** Scrolls by {@code delta} (positive is down); true when the offset changed. */
    public boolean scrollBy(int delta) {
        int before = offset;
        setOffset(offset + delta);
        return offset != before;
    }

    public boolean scrolls() {
        return ListScroll.scrolls(total, visible);
    }

    /** Whether the point is on the bar, its frame included. */
    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x - 1 && mouseX < x + width + 1 && mouseY >= y - 1 && mouseY < y + height + 1;
    }

    private int thumbStart() {
        return ListScroll.thumbStart(offset, total, visible, height);
    }

    private int thumbLength() {
        return ListScroll.thumbLength(total, visible, height);
    }

    private int maxOffset() {
        return Math.max(0, total - visible);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, LIGHT);
        graphics.fill(x - 1, y - 1, x + width, y + height, DARK);
        graphics.fill(x, y, x + width, y + height, TRACK);
        if (!scrolls()) {
            graphics.fill(x, y, x + width, y + height, GREYED);
            return;
        }
        int top = y + thumbStart();
        int bottom = top + thumbLength();
        boolean lit = dragging || mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < bottom;
        graphics.fill(x, top, x + width, bottom, SHADOW);
        graphics.fill(x, top, x + width - 1, bottom - 1, LIGHT);
        graphics.fill(x + 1, top + 1, x + width - 1, bottom - 1, lit ? FACE_HOVERED : FACE);
    }

    /**
     * A left press on the bar grabs the thumb where pressed, or centres it on
     * a press on the track, and starts the drag. True when the press was on
     * the bar, also when nothing scrolls.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        if (scrolls()) {
            grab = ListScroll.grabAt(mouseY - y, thumbStart(), thumbLength());
            dragging = true;
            drag(mouseY);
        }
        return true;
    }

    /** Moves the held thumb with the mouse; true while the bar holds a drag. */
    public boolean mouseDragged(double mouseY) {
        if (!dragging) {
            return false;
        }
        drag(mouseY);
        return true;
    }

    /** Ends a drag; true when one was held. */
    public boolean mouseReleased() {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    private void drag(double mouseY) {
        offset = ListScroll.offsetFor(mouseY - y, grab, thumbLength(), height, maxOffset());
    }
}
