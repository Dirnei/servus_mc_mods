package io.github.dirnei.mc.servus.storage.rack;

import java.util.function.IntPredicate;

/**
 * Whether a rack section is docked: a section is closed to pipes and hoppers
 * as soon as a warehouse rail stands in front of any of its bays, because
 * from then on only shuttles serve it. Pure, no Minecraft imports.
 */
public final class RackDock {

    private RackDock() {
    }

    /**
     * @param offsets   the section's bays as offsets, as {@link RackSection#bays} gives them
     * @param frontRail whether a rail stands in front of the bay at an offset
     * @return whether any bay of the section has a rail in front
     */
    public static boolean closed(int[] offsets, IntPredicate frontRail) {
        for (int offset : offsets) {
            if (frontRail.test(offset)) {
                return true;
            }
        }
        return false;
    }
}
