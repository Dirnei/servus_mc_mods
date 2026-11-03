package io.github.dirnei.mc.servus.notes.client;

import java.util.ArrayList;
import java.util.List;

/**
 * The clickable areas of the notebook pages, collected while a frame is drawn and clicked against
 * until the next frame. An area added later lies on top of earlier ones.
 */
final class PageHits {

    private record Hit(int x0, int y0, int x1, int y1, Runnable action) {

        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    private final List<Hit> hits = new ArrayList<>();

    void clear() {
        hits.clear();
    }

    void add(int x, int y, int width, int height, Runnable action) {
        hits.add(new Hit(x, y, x + width, y + height, action));
    }

    /** Runs the top-most area under the point; false when there is none. */
    boolean click(double x, double y) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(x, y)) {
                hits.get(i).action().run();
                return true;
            }
        }
        return false;
    }

    static boolean over(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
