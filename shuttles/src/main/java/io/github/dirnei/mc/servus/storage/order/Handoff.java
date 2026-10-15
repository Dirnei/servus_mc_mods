package io.github.dirnei.mc.servus.storage.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides where the outputs of one step go when later steps of the same order
 * need them: straight to later steps that are delivering on another producer,
 * into the CPU's buffer for later steps that are not delivering yet (same
 * producer or busy producer) as far as it has room, held for a while for a
 * busy producer when the buffer could not take them, the rest to storage.
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
     * @param buffered  how many go into the CPU's buffer
     * @param hold      how many stay at the producer for now
     * @param toStorage how many go to storage
     * @param overflow  the part of {@code toStorage} that later steps still owed
     */
    public record Split(List<Integer> handed, int buffered, int hold, int toStorage, int overflow) {
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
     * @param bufferRoom how many of the key the order can still put into the CPU's buffer
     * @param bufferHeld how many of the key the order already has in the buffer or on their way in
     * @param heldFor    ticks the outputs have waited so far, 0 when they just became ready
     * @param waitLimit  ticks outputs may wait for a busy dependant
     */
    public static Split split(int movable, List<Dependant> dependants, int bufferRoom, int bufferHeld,
                              long heldFor, long waitLimit) {
        List<Integer> handed = new ArrayList<>(dependants.size());
        int remaining = Math.max(0, movable);
        int busyOwed = 0;
        int sameOwed = 0;
        for (Dependant dependant : dependants) {
            int give = 0;
            if (dependant.owed() > 0) {
                if (dependant.sameProducer()) {
                    sameOwed += dependant.owed();
                } else if (dependant.delivering()) {
                    give = Math.min(remaining, dependant.owed());
                } else {
                    busyOwed += dependant.owed();
                }
            }
            handed.add(give);
            remaining -= give;
        }
        int wanting = Math.max(0, busyOwed + sameOwed - Math.max(0, bufferHeld));
        int buffered = Math.min(remaining, Math.min(wanting, Math.max(0, bufferRoom)));
        remaining -= buffered;
        int busyRest = Math.max(0, Math.min(busyOwed, wanting - buffered));
        int hold = 0;
        if (remaining > 0 && busyRest > 0 && Math.max(0, heldFor) < waitLimit) {
            hold = Math.min(remaining, busyRest);
        }
        int toStorage = remaining - hold;
        int overflow = Math.min(toStorage, Math.max(0, wanting - buffered - hold));
        return new Split(Collections.unmodifiableList(handed), buffered, hold, toStorage, overflow);
    }
}
