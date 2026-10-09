package io.github.dirnei.mc.servus.storage.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides where the outputs of one step go when later steps of the same order
 * need them: straight to later steps that are delivering on another producer,
 * held for a while for later steps whose producer is still busy, the rest to
 * storage. Pure, no Minecraft imports.
 */
public final class Handoff {

    private Handoff() {
    }

    /**
     * A later step of the same order that needs the output.
     *
     * @param owed         how many it still needs
     * @param delivering   whether it runs on its producer and takes inputs now
     * @param sameProducer whether it runs on the producer that holds the outputs
     */
    public record Dependant(int owed, boolean delivering, boolean sameProducer) {
    }

    /**
     * @param handed    how many go to each dependant, in the order given
     * @param hold      how many stay at the producer for now
     * @param toStorage how many go to storage
     */
    public record Split(List<Integer> handed, int hold, int toStorage) {
        public int handedTotal() {
            int sum = 0;
            for (int count : handed) {
                sum += count;
            }
            return sum;
        }
    }

    /**
     * @param movable    outputs that can leave the producer now
     * @param dependants later steps of the order that use the output
     * @param heldFor    ticks the outputs have waited so far, 0 when they just became ready
     * @param waitLimit  ticks outputs may wait for a busy dependant
     */
    public static Split split(int movable, List<Dependant> dependants, long heldFor, long waitLimit) {
        List<Integer> handed = new ArrayList<>(dependants.size());
        int remaining = Math.max(0, movable);
        int waitingOwed = 0;
        for (Dependant dependant : dependants) {
            int give = 0;
            if (dependant.owed() > 0 && !dependant.sameProducer()) {
                if (dependant.delivering()) {
                    give = Math.min(remaining, dependant.owed());
                } else {
                    waitingOwed += dependant.owed();
                }
            }
            handed.add(give);
            remaining -= give;
        }
        int hold = 0;
        if (remaining > 0 && waitingOwed > 0 && Math.max(0, heldFor) < waitLimit) {
            hold = Math.min(remaining, waitingOwed);
        }
        return new Split(Collections.unmodifiableList(handed), hold, remaining - hold);
    }
}
