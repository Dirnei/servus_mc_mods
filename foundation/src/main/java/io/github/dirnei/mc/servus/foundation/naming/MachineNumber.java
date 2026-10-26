package io.github.dirnei.mc.servus.foundation.naming;

import java.util.Objects;
import java.util.UUID;

/** When a warehouse numbers a machine. */
public final class MachineNumber {

    private MachineNumber() {
    }

    /**
     * Whether a machine needs a number from the current warehouse: it has none,
     * or the warehouse that gave it is another one.
     */
    public static boolean needsNumber(UUID numberedBy, int number, UUID current) {
        return number <= 0 || numberedBy == null || !Objects.equals(numberedBy, current);
    }
}
