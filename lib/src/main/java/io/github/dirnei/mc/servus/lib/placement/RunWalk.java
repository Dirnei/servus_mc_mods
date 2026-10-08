package io.github.dirnei.mc.servus.lib.placement;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Walks a straight run of blocks to the first spot past its end. Pure Java,
 * so positions are plain integer cells.
 */
public final class RunWalk {

    /** A block position or a step between positions. */
    public record Cell(int x, int y, int z) {

        public Cell plus(Cell step, int times) {
            return new Cell(x + step.x * times, y + step.y * times, z + step.z * times);
        }
    }

    private RunWalk() {
    }

    /**
     * Steps from {@code start} while the next cell is part of the run. The first
     * cell that is not is the target when it is free and at most {@code maxReach}
     * steps from the start. The start itself is never tested.
     *
     * @return the target, or empty when the run ends at a blocked cell or runs past the reach
     */
    public static Optional<Cell> walk(Cell start, Cell step, Predicate<Cell> isRun, Predicate<Cell> isFree, int maxReach) {
        for (int distance = 1; distance <= maxReach; distance++) {
            Cell cell = start.plus(step, distance);
            if (isRun.test(cell)) {
                continue;
            }
            return isFree.test(cell) ? Optional.of(cell) : Optional.empty();
        }
        return Optional.empty();
    }
}
