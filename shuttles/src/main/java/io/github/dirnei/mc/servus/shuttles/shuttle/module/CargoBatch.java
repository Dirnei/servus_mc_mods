package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Which queued jobs ride along with a shuttle that has room for more. */
public final class CargoBatch {

    private CargoBatch() {
    }

    /** Indexes of the first room entries of the queue that share the way, in queue order. */
    public static <T> List<Integer> take(List<T> queue, Predicate<T> sameWay, int room) {
        List<Integer> taken = new ArrayList<>();
        for (int i = 0; i < queue.size() && taken.size() < room; i++) {
            if (sameWay.test(queue.get(i))) {
                taken.add(i);
            }
        }
        return taken;
    }
}
