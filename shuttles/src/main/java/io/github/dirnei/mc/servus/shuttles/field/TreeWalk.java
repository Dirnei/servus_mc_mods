package io.github.dirnei.mc.servus.shuttles.field;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Which blocks felling a tree removes, from a lookup of block kinds: the logs
 * connected to the base through all 26 neighbours, down to the ground too,
 * that are nearer to this trunk than to the trunk of another spot or square,
 * see {@link #NEIGHBOUR_REACH}; the natural leaves those logs
 * held that no other log holds within leaf reach, so nothing is left to decay;
 * and the hanging parts touching anything removed. Pure Java.
 */
public final class TreeWalk {

    /** How far from the base, in x and z, the walk looks for the trunks of other trees on the base level. */
    public static final int NEIGHBOUR_REACH = 8;

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

    /** No log on the base, or logs without natural leaves near them that touch no other tree, such as a pillar a player stacked. */
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
        Claims claims = claims(base, kind, otherBase, maxLogs);
        if (claims.tooMany) {
            return new TooLarge();
        }
        Set<FieldPos> logs = claims.own;
        Set<FieldPos> candidates = leavesWithinReach(logs, kind, Set.of());
        if (logs.isEmpty() || candidates.isEmpty() && !claims.touchesOther) {
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

    /**
     * Which logs are this tree's. This tree grows from its base logs and every
     * other tree from its base logs on the base level within
     * {@link #NEIGHBOUR_REACH}, all at once over connected logs, and each log
     * goes to the tree that reaches it first: by the fewest steps that do
     * not climb, then the fewest steps sideways, then the fewest steps, the
     * other trees on a tie. Trees grow upward, so a trunk reaches its own
     * straight logs first, and every log left standing still hangs on
     * another trunk.
     */
    private static Claims claims(Collection<FieldPos> base, Function<FieldPos, Kind> kind, Predicate<FieldPos> otherBase, int maxLogs) {
        int baseY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (FieldPos plot : base) {
            baseY = Math.min(baseY, plot.y());
            minX = Math.min(minX, plot.x());
            maxX = Math.max(maxX, plot.x());
            minZ = Math.min(minZ, plot.z());
            maxZ = Math.max(maxZ, plot.z());
        }
        Claims claims = new Claims(kind, maxLogs);
        for (FieldPos plot : base) {
            claims.offer(plot, true, 0, 0, 0);
        }
        if (claims.ownPending == 0) {
            return claims;
        }
        for (int x = minX - NEIGHBOUR_REACH; x <= maxX + NEIGHBOUR_REACH; x++) {
            for (int z = minZ - NEIGHBOUR_REACH; z <= maxZ + NEIGHBOUR_REACH; z++) {
                FieldPos plot = new FieldPos(x, baseY, z);
                if (!claims.owner.containsKey(plot) && otherBase.test(plot)) {
                    claims.offer(plot, false, 0, 0, 0);
                }
            }
        }
        while (claims.ownPending > 0 && !claims.tooMany) {
            claims.next(baseY, otherBase);
        }
        return claims;
    }

    /**
     * The logs claimed so far and by which side, the logs waiting to grow on
     * in the order they were reached, and whether this tree touches another
     * one: its logs then count as a tree even when the other tree's logs hold
     * all the leaves.
     */
    private static final class Claims {

        private record Step(FieldPos log, boolean mine, int flat, int bends, int steps) {
        }

        private static final Comparator<Step> ORDER = Comparator.comparingInt(Step::flat).thenComparingInt(Step::bends)
                .thenComparingInt(Step::steps)
                .thenComparing(Step::mine);

        final Function<FieldPos, Kind> kind;
        final int maxLogs;
        final Map<FieldPos, Boolean> owner = new HashMap<>();
        final Set<FieldPos> own = new LinkedHashSet<>();
        final PriorityQueue<Step> queue = new PriorityQueue<>(ORDER);
        int ownPending;
        boolean tooMany;
        boolean touchesOther;

        Claims(Function<FieldPos, Kind> kind, int maxLogs) {
            this.kind = kind;
            this.maxLogs = maxLogs;
        }

        /** Queues a log for that side, reached with that many steps that do not climb, that move sideways and in all. */
        void offer(FieldPos log, boolean mine, int flat, int bends, int steps) {
            if (kind.apply(log) != Kind.LOG) {
                return;
            }
            queue.add(new Step(log, mine, flat, bends, steps));
            if (mine) {
                ownPending++;
            }
        }

        /** Claims the first waiting log not claimed yet and queues the logs touching it; a log on another base is another trunk's. */
        void next(int baseY, Predicate<FieldPos> otherBase) {
            Step step = queue.poll();
            if (step.mine()) {
                ownPending--;
            }
            Boolean held = owner.get(step.log());
            if (held != null) {
                touchesOther |= held != step.mine();
                return;
            }
            owner.put(step.log(), step.mine());
            if (step.mine()) {
                own.add(step.log());
                tooMany = own.size() > maxLogs;
            }
            FieldPos log = step.log();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        FieldPos next = new FieldPos(log.x() + dx, log.y() + dy, log.z() + dz);
                        Boolean nextHeld = owner.get(next);
                        if (nextHeld != null) {
                            touchesOther |= nextHeld != step.mine();
                            continue;
                        }
                        boolean mine = step.mine() && !(next.y() == baseY && otherBase.test(next));
                        offer(next, mine, step.flat() + (dy > 0 ? 0 : 1), step.bends() + (dx != 0 || dz != 0 ? 1 : 0), step.steps() + 1);
                    }
                }
            }
        }
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
