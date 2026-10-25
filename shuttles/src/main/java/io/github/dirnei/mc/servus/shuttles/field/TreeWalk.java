package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which blocks felling a tree removes, from a lookup of block kinds: the logs
 * connected to the base through all 26 neighbours, never below the base and
 * never into another spot's or square's base; the natural leaves those logs
 * held that no other log holds within leaf reach, so nothing is left to decay;
 * and the hanging parts touching anything removed. Pure Java.
 */
public final class TreeWalk {

    /** How far leaves reach from a log before they decay, in face steps through leaves. */
    public static final int LEAF_REACH = 6;

    private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** What a block is to the walk. */
    public enum Kind { LOG, NATURAL_LEAVES, ATTACHMENT, OTHER }

    /** What felling the tree comes to. */
    public sealed interface Result permits Felled, TooLarge, NotATree {
    }

    /** The blocks to remove, each set in walk order. */
    public record Felled(Set<FieldPos> logs, Set<FieldPos> leaves, Set<FieldPos> attachments) implements Result {

        public int size() {
            return logs.size() + leaves.size() + attachments.size();
        }
    }

    /** More logs or blocks than the limits allow. */
    public record TooLarge() implements Result {
    }

    /** No log on the base, or logs without natural leaves near them, such as a pillar a player stacked. */
    public record NotATree() implements Result {
    }

    private TreeWalk() {
    }

    /**
     * @param base       the plots the tree stands on: one, or the four of a square
     * @param kinds      the kind of the block at a position
     * @param otherBase  whether a position on the base level is the base of another spot or square
     * @param maxLogs    the most logs a felled tree may have
     * @param maxBlocks  the most blocks in all a felled tree may have
     */
    public static Result walk(Collection<FieldPos> base, Function<FieldPos, Kind> kinds, Predicate<FieldPos> otherBase,
                              int maxLogs, int maxBlocks) {
        Map<FieldPos, Kind> seen = new HashMap<>();
        Function<FieldPos, Kind> kind = pos -> seen.computeIfAbsent(pos, kinds);
        Set<FieldPos> logs = logs(base, kind, otherBase, maxLogs);
        if (logs == null) {
            return new TooLarge();
        }
        if (logs.isEmpty()) {
            return new NotATree();
        }
        Set<FieldPos> candidates = leavesWithinReach(logs, kind, Set.of());
        if (candidates.isEmpty()) {
            return new NotATree();
        }
        if (logs.size() + candidates.size() > maxBlocks) {
            return new TooLarge();
        }
        Set<FieldPos> leaves = new LinkedHashSet<>(candidates);
        leaves.removeAll(leavesWithinReach(holders(candidates, logs, kind), kind, candidates));
        Set<FieldPos> attachments = attachments(logs, leaves, kind, maxBlocks - logs.size() - leaves.size());
        if (attachments == null) {
            return new TooLarge();
        }
        return new Felled(logs, leaves, attachments);
    }

    /** The logs connected to the base logs, or null past the limit. */
    private static Set<FieldPos> logs(Collection<FieldPos> base, Function<FieldPos, Kind> kind, Predicate<FieldPos> otherBase, int maxLogs) {
        int baseY = Integer.MAX_VALUE;
        for (FieldPos plot : base) {
            baseY = Math.min(baseY, plot.y());
        }
        Set<FieldPos> logs = new LinkedHashSet<>();
        ArrayDeque<FieldPos> queue = new ArrayDeque<>();
        for (FieldPos plot : base) {
            if (kind.apply(plot) == Kind.LOG && logs.add(plot)) {
                queue.add(plot);
            }
        }
        while (!queue.isEmpty()) {
            FieldPos log = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        FieldPos next = new FieldPos(log.x() + dx, log.y() + dy, log.z() + dz);
                        if (next.y() < baseY || next.y() == baseY && otherBase.test(next) || logs.contains(next)
                                || kind.apply(next) != Kind.LOG) {
                            continue;
                        }
                        logs.add(next);
                        if (logs.size() > maxLogs) {
                            return null;
                        }
                        queue.add(next);
                    }
                }
            }
        }
        return logs;
    }

    /** The natural leaves within leaf reach of the sources, through leaves only; positions in {@code only} limit it when not empty. */
    private static Set<FieldPos> leavesWithinReach(Set<FieldPos> sources, Function<FieldPos, Kind> kind, Set<FieldPos> only) {
        Set<FieldPos> reached = new LinkedHashSet<>();
        Map<FieldPos, Integer> depth = new HashMap<>();
        ArrayDeque<FieldPos> queue = new ArrayDeque<>();
        for (FieldPos source : sources) {
            depth.put(source, 0);
            queue.add(source);
        }
        while (!queue.isEmpty()) {
            FieldPos at = queue.poll();
            int next = depth.get(at) + 1;
            if (next > LEAF_REACH) {
                continue;
            }
            for (int[] face : FACES) {
                FieldPos leaf = new FieldPos(at.x() + face[0], at.y() + face[1], at.z() + face[2]);
                if (depth.containsKey(leaf) || kind.apply(leaf) != Kind.NATURAL_LEAVES) {
                    continue;
                }
                depth.put(leaf, next);
                reached.add(leaf);
                queue.add(leaf);
            }
        }
        if (!only.isEmpty()) {
            reached.retainAll(only);
        }
        return reached;
    }

    /** The logs that are not felled and lie within leaf reach of a candidate leaf: the only ones that can still hold it. */
    private static Set<FieldPos> holders(Set<FieldPos> candidates, Set<FieldPos> felled, Function<FieldPos, Kind> kind) {
        Set<FieldPos> holders = new LinkedHashSet<>();
        Map<FieldPos, Integer> depth = new HashMap<>();
        ArrayDeque<FieldPos> queue = new ArrayDeque<>();
        for (FieldPos leaf : candidates) {
            depth.put(leaf, 1);
            queue.add(leaf);
        }
        while (!queue.isEmpty()) {
            FieldPos at = queue.poll();
            int reach = depth.get(at);
            for (int[] face : FACES) {
                FieldPos next = new FieldPos(at.x() + face[0], at.y() + face[1], at.z() + face[2]);
                if (depth.containsKey(next)) {
                    continue;
                }
                Kind found = kind.apply(next);
                if (found == Kind.LOG && !felled.contains(next)) {
                    holders.add(next);
                } else if (found == Kind.NATURAL_LEAVES && reach < LEAF_REACH) {
                    depth.put(next, reach + 1);
                    queue.add(next);
                }
            }
        }
        return holders;
    }

    /** The hanging parts touching a removed block or another such part, or null past the room left. */
    private static Set<FieldPos> attachments(Set<FieldPos> logs, Set<FieldPos> leaves, Function<FieldPos, Kind> kind, int room) {
        Set<FieldPos> attachments = new LinkedHashSet<>();
        ArrayDeque<FieldPos> queue = new ArrayDeque<>(logs);
        queue.addAll(leaves);
        while (!queue.isEmpty()) {
            FieldPos at = queue.poll();
            for (int[] face : FACES) {
                FieldPos next = new FieldPos(at.x() + face[0], at.y() + face[1], at.z() + face[2]);
                if (attachments.contains(next) || kind.apply(next) != Kind.ATTACHMENT) {
                    continue;
                }
                attachments.add(next);
                if (attachments.size() > room) {
                    return null;
                }
                queue.add(next);
            }
        }
        return attachments;
    }
}
