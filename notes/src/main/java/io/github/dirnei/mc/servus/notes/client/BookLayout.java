package io.github.dirnei.mc.servus.notes.client;

/**
 * Where the notebook lies on the screen and how big it is, in GUI pixels. The book grows with the
 * screen up to {@link #MAX_WIDTH} by {@link #MAX_HEIGHT} and never lays out smaller than
 * {@link #MIN_WIDTH} by {@link #MIN_HEIGHT}; only a window too small for that scales it down.
 *
 * @param left   screen x of the cover's left edge
 * @param top    screen y of the cover's top edge; the tabs stand above it
 * @param scale  1 unless the window is too small for the smallest book
 * @param width  the cover's width in book pixels
 * @param height the cover's height in book pixels
 */
public record BookLayout(int left, int top, float scale, int width, int height) {

    public static final int MIN_WIDTH = 336;
    public static final int MIN_HEIGHT = 208;
    public static final int MAX_WIDTH = 576;
    public static final int MAX_HEIGHT = 328;
    public static final int SCREEN_MARGIN = 8;
    /** How far the tabs stand above the cover. */
    public static final int TAB_RISE = 15;
    /** The room kept free right of the book for JEI's item list while an item is picked. */
    public static final int SIDE_ROOM = 170;
    /** The leather around the pages: left and right of them, and above and below. */
    public static final int COVER_SIDE = 12;
    public static final int COVER_TOP = 11;

    /**
     * The book for a screen of the given size; with {@code roomRight} it keeps {@link #SIDE_ROOM} free on
     * the right when the screen allows and lies at the left margin instead of the middle.
     */
    public static BookLayout fit(int screenWidth, int screenHeight, boolean roomRight) {
        int availableWidth = screenWidth - 2 * SCREEN_MARGIN;
        int availableHeight = screenHeight - 2 * SCREEN_MARGIN - TAB_RISE;
        int width = Math.clamp(availableWidth - (roomRight ? SIDE_ROOM : 0), MIN_WIDTH, MAX_WIDTH);
        int height = Math.clamp(availableHeight, MIN_HEIGHT, MAX_HEIGHT);
        float scale = Math.min(1F, Math.min((float) availableWidth / width, (float) (availableHeight + TAB_RISE) / (height + TAB_RISE)));
        scale = Math.max(scale, 0.1F);
        int left = roomRight ? SCREEN_MARGIN : Math.round((screenWidth - width * scale) / 2F);
        int top = Math.round((screenHeight - (height + TAB_RISE) * scale) / 2F + TAB_RISE * scale);
        return new BookLayout(left, top, scale, width, height);
    }

    public int pageY() {
        return COVER_TOP;
    }

    public int pageHeight() {
        return height - 2 * COVER_TOP;
    }

    public int leftPageX() {
        return COVER_SIDE;
    }

    /** Two fifths of the paper go to the left page. */
    public int leftPageWidth() {
        return (width - 2 * COVER_SIDE) * 2 / 5;
    }

    /** The spine, where the right page starts. */
    public int rightPageX() {
        return leftPageX() + leftPageWidth();
    }

    public int rightPageWidth() {
        return width - COVER_SIDE - rightPageX();
    }

    /** The bottom edge of the pages. */
    public int pageBottom() {
        return pageY() + pageHeight();
    }
}
