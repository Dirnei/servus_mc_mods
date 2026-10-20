package io.github.dirnei.mc.servus.brewery.crop;

import java.util.List;
import java.util.Optional;

/**
 * The growth rules of a hop vine on a column of Hop Poles. A column is given
 * as the stages of its parts from the foot (index 0) up.
 */
public final class HopVine {

    /** The most poles a column may have. */
    public static final int MAX_HEIGHT = 3;

    private HopVine() {
    }

    /** Whether a pole may be placed on top of {@code polesBelow} poles standing on each other. */
    public static boolean canPlaceOn(int polesBelow) {
        return polesBelow < MAX_HEIGHT;
    }

    /**
     * The step a growth tick on {@code part} makes: a young part leafs out, a
     * leafed part climbs onto a bare pole above it, and once the top part has
     * leafed out every leafed part above the foot grows cones.
     */
    public static Optional<HopStep> growthAt(List<HopStage> column, int part) {
        HopStage stage = column.get(part);
        if (stage == HopStage.YOUNG) {
            return Optional.of(new HopStep(part, HopStage.GROWN));
        }
        if (stage == HopStage.BARE) {
            return Optional.empty();
        }
        int above = part + 1;
        if (above < column.size() && column.get(above) == HopStage.BARE) {
            return Optional.of(new HopStep(above, HopStage.YOUNG));
        }
        if (stage == HopStage.GROWN && part > 0 && topHasLeafedOut(column)) {
            return Optional.of(new HopStep(part, HopStage.CONES));
        }
        return Optional.empty();
    }

    /** The first step any part of the column can make, from the foot up. */
    public static Optional<HopStep> firstGrowth(List<HopStage> column) {
        for (int part = 0; part < column.size(); part++) {
            Optional<HopStep> step = growthAt(column, part);
            if (step.isPresent()) {
                return step;
            }
        }
        return Optional.empty();
    }

    private static boolean topHasLeafedOut(List<HopStage> column) {
        HopStage top = column.getLast();
        return top == HopStage.GROWN || top == HopStage.CONES;
    }
}
