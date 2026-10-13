package io.github.dirnei.mc.servus.storage.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import io.github.dirnei.mc.servus.storage.stats.StorageStatsManager;
import io.github.dirnei.mc.servus.storage.rail.RailBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * The loaded Field Heads of each level, so rail changes and crop events reach
 * the heads they concern without any block scan. Server thread only.
 */
public final class FieldHeads {

    private static final Map<Level, Set<FieldHeadBlockEntity>> HEADS = new WeakHashMap<>();

    private FieldHeads() {
    }

    static void add(FieldHeadBlockEntity head) {
        HEADS.computeIfAbsent(head.getLevel(), level -> new LinkedHashSet<>()).add(head);
    }

    static void remove(FieldHeadBlockEntity head) {
        Set<FieldHeadBlockEntity> heads = HEADS.get(head.getLevel());
        if (heads != null) {
            heads.remove(head);
        }
    }

    /** The loaded heads of the level, as a copy. */
    public static List<FieldHeadBlockEntity> heads(Level level) {
        Set<FieldHeadBlockEntity> heads = HEADS.get(level);
        return heads == null ? List.of() : new ArrayList<>(heads);
    }

    /** The head serving the field that holds this farm rail. */
    public static Optional<FieldHeadBlockEntity> servingAt(Level level, BlockPos railPos) {
        for (FieldHeadBlockEntity head : heads(level)) {
            if (head.status() == FieldStatus.SERVING && head.rails().contains(railPos)) {
                return Optional.of(head);
            }
        }
        return Optional.empty();
    }

    /** The serving head whose plots hold that position. */
    public static Optional<FieldHeadBlockEntity> coveringPlot(Level level, BlockPos pos) {
        Set<FieldHeadBlockEntity> heads = HEADS.getOrDefault(level, Collections.emptySet());
        for (FieldHeadBlockEntity head : heads) {
            if (head.covers(pos)) {
                return Optional.of(head);
            }
        }
        return Optional.empty();
    }

    /** A farm rail was placed or removed: heads whose field holds it or touches it, and heads beside it, look again. */
    public static void railChanged(Level level, BlockPos pos) {
        for (FieldHeadBlockEntity head : heads(level)) {
            if (concerns(head, pos)) {
                head.requestRefresh();
            }
        }
    }

    /** Whether the head's field holds or touches the rail position, or the head stands beside it. */
    private static boolean concerns(FieldHeadBlockEntity head, BlockPos pos) {
        return head.touchesRail(pos) || head.getBlockPos().getY() == pos.getY() && head.getBlockPos().distManhattan(pos) == 1;
    }

    static boolean isFarmRail(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof FarmRailBlock;
    }

    /** The loaded farm rails beside the rail, north, east, south, west. */
    static List<BlockPos> farmNeighbours(Level level, BlockPos rail) {
        List<BlockPos> neighbours = new ArrayList<>(4);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos next = rail.relative(direction);
            if (isFarmRail(level, next)) {
                neighbours.add(next);
            }
        }
        return neighbours;
    }

    /** The farm rails of the field that holds this rail, walked over loaded farm rails up to {@code max_rails}. */
    public static FieldGraph.Result<BlockPos> field(Level level, BlockPos rail) {
        return FieldGraph.collect(rail.immutable(), current -> farmNeighbours(level, current), StorageStatsManager.INSTANCE.fieldMaxRails());
    }

    /**
     * A loose farm rail (in no field yet) was placed beside farm rails that
     * have a stage. When a head looks after that field, its refresh sets the
     * stage; else the joined field takes the youngest stage of its rails.
     */
    static void loosePlaced(Level level, BlockPos pos) {
        for (FieldHeadBlockEntity head : heads(level)) {
            if (concerns(head, pos)) {
                return;
            }
        }
        FieldGraph.Result<BlockPos> field = field(level, pos);
        if (field.tooLarge()) {
            return;
        }
        List<RailBlockEntity> members = new ArrayList<>(field.members().size());
        List<Integer> stages = new ArrayList<>(field.members().size());
        for (BlockPos rail : field.members()) {
            if (level.getBlockEntity(rail) instanceof RailBlockEntity entity) {
                members.add(entity);
                stages.add(entity.corrosion());
            }
        }
        int joined = FieldCorrosion.join(FieldCorrosion.LOOSE, stages);
        if (joined != FieldCorrosion.LOOSE) {
            members.forEach(entity -> entity.setCorrosion(joined));
        }
    }

    /** A head was removed: the heads that share a rail with its field look again, one of them may serve it now. */
    static void headRemoved(Level level, FieldHeadBlockEntity removed) {
        for (FieldHeadBlockEntity head : heads(level)) {
            if (head != removed && sharesField(head, removed)) {
                head.requestRefresh();
            }
        }
    }

    private static boolean sharesField(FieldHeadBlockEntity head, FieldHeadBlockEntity other) {
        for (BlockPos rail : other.rails()) {
            if (head.rails().contains(rail)) {
                return true;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (rail.relative(direction).equals(head.getBlockPos())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A chunk loaded: heads whose field reaches into it look again. */
    public static void chunkLoaded(Level level, ChunkPos chunk) {
        for (FieldHeadBlockEntity head : heads(level)) {
            if (head.reaches(chunk)) {
                head.requestRefresh();
            }
        }
    }
}
