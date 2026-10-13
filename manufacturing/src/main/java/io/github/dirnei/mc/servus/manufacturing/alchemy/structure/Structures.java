package io.github.dirnei.mc.servus.manufacturing.alchemy.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** The bridge from the pure build geometry to world positions and directions. */
public final class Structures {

    /** The result of checking a build in its best facing. */
    public record Check<C>(Facing facing, Optional<C> firstWrong) {

        public boolean complete() {
            return firstWrong.isEmpty();
        }
    }

    private Structures() {
    }

    public static Facing facing(Direction direction) {
        return switch (direction) {
            case EAST -> Facing.EAST;
            case SOUTH -> Facing.SOUTH;
            case WEST -> Facing.WEST;
            default -> Facing.NORTH;
        };
    }

    public static Direction direction(Facing facing) {
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case EAST -> Direction.EAST;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
        };
    }

    /** The position {@code right} and {@code up} from {@code origin} for a front facing {@code facing}. */
    public static BlockPos pos(BlockPos origin, int right, int up, Facing facing) {
        return pos(origin, facing.offset(right, up));
    }

    public static BlockPos pos(BlockPos origin, Offset offset) {
        return origin.offset(offset.x(), offset.y(), offset.z());
    }

    /**
     * Checks {@code cells} in every facing, {@code own} first, and returns the first complete facing,
     * else the one where most cells match.
     */
    public static <C> Check<C> check(Direction own, List<C> cells, BiPredicate<Facing, C> matches) {
        List<Facing> order = new ArrayList<>(List.of(Facing.values()));
        Facing ownFacing = facing(own);
        order.remove(ownFacing);
        order.addFirst(ownFacing);
        return check(order, cells, matches);
    }

    /**
     * Checks {@code cells} in the facings of {@code order} and returns the first complete facing,
     * else the first one where most cells match.
     */
    public static <C> Check<C> check(List<Facing> order, List<C> cells, BiPredicate<Facing, C> matches) {
        Check<C> best = null;
        int bestMatches = -1;
        for (Facing facing : order) {
            Optional<C> firstWrong = Optional.empty();
            int matched = 0;
            for (C cell : cells) {
                if (matches.test(facing, cell)) {
                    matched++;
                } else if (firstWrong.isEmpty()) {
                    firstWrong = Optional.of(cell);
                }
            }
            if (firstWrong.isEmpty()) {
                return new Check<>(facing, firstWrong);
            }
            if (matched > bestMatches) {
                bestMatches = matched;
                best = new Check<>(facing, firstWrong);
            }
        }
        return best;
    }
}
