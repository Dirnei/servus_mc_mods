package io.github.dirnei.mc.servus.logistics.tank;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Splits a connected group of tank blocks into tanks that each fill a box.
 * Every block belongs to exactly one box; a lone block is a one block box.
 * Two touching boxes whose touching faces match merge, until no pair can.
 * Removing a block breaks its box into lone blocks, which merge again.
 *
 * <p>A block that comes back from a save with the bounds of its box waits as
 * a lone block, merging with nothing, until every block of that box is
 * present with the same bounds; then the box is restored. When a missing
 * block can no longer arrive, the waiting blocks are released. Pure Java.
 */
public final class TankBoxes {

    /** What the owner knows about the world. */
    public interface Rules {

        /** Whether a block that is not in the group yet may still join it (for example an unloaded chunk). */
        boolean mayArrive(NodePos pos);

        /** Whether two boxes may become one tank (for example both hold the same fluid). */
        boolean mayMerge(TankShape a, TankShape b);
    }

    private final Map<NodePos, TankShape> boxOf = new LinkedHashMap<>();
    /** Blocks waiting for their saved box, with that box. */
    private final Map<NodePos, TankShape> waiting = new HashMap<>();
    private Rules rules;

    public TankBoxes(Rules rules) {
        this.rules = rules;
    }

    public void setRules(Rules rules) {
        this.rules = rules;
    }

    /** The box of a block in the group, or null. */
    public TankShape boxOf(NodePos pos) {
        return boxOf.get(pos);
    }

    public boolean contains(NodePos pos) {
        return boxOf.containsKey(pos);
    }

    public boolean isWaiting(NodePos pos) {
        return waiting.containsKey(pos);
    }

    public boolean hasWaiting() {
        return !waiting.isEmpty();
    }

    /** Every box once, in the order their first block joined. */
    public Set<TankShape> boxes() {
        return new LinkedHashSet<>(boxOf.values());
    }

    public int size() {
        return boxOf.size();
    }

    /**
     * Adds a block. {@code saved} is the box the block belonged to when it was
     * saved, or null; a saved box of one block counts as none.
     */
    public void add(NodePos pos, TankShape saved) {
        TankShape single = TankShape.single(pos);
        boxOf.put(pos, single);
        if (saved != null && saved.volume() > 1 && saved.contains(pos)) {
            waiting.put(pos, saved);
            recheck();
        } else {
            mergeFrom(List.of(single));
        }
    }

    /** Removes a block; the rest of its box falls apart into lone blocks that merge again. */
    public void remove(NodePos pos) {
        TankShape box = boxOf.remove(pos);
        waiting.remove(pos);
        if (box == null) {
            return;
        }
        List<TankShape> loose = new ArrayList<>();
        if (box.volume() > 1) {
            for (NodePos member : box.positions()) {
                if (boxOf.containsKey(member)) {
                    TankShape single = TankShape.single(member);
                    boxOf.put(member, single);
                    loose.add(single);
                }
            }
        }
        mergeFrom(loose);
        recheck();
    }

    /** Takes over the blocks of another group that joined this one. */
    public void absorb(TankBoxes other) {
        boxOf.putAll(other.boxOf);
        waiting.putAll(other.waiting);
        recheck();
        mergeFrom(boxes());
    }

    /**
     * Restores saved boxes that are complete and releases waiting blocks whose
     * box can no longer complete. Returns true when anything changed.
     */
    public boolean recheck() {
        if (waiting.isEmpty()) {
            return false;
        }
        boolean changed = false;
        List<TankShape> ready = new ArrayList<>();
        Set<TankShape> released = new LinkedHashSet<>();
        Set<TankShape> seen = new LinkedHashSet<>(waiting.values());
        for (TankShape saved : seen) {
            boolean complete = true;
            boolean possible = true;
            for (NodePos member : saved.positions()) {
                if (!saved.equals(waiting.get(member))) {
                    complete = false;
                    if (boxOf.containsKey(member) || !rules.mayArrive(member)) {
                        possible = false;
                        break;
                    }
                }
            }
            if (complete) {
                ready.add(saved);
            } else if (!possible) {
                released.add(saved);
            }
        }
        for (TankShape saved : ready) {
            for (NodePos member : saved.positions()) {
                waiting.remove(member);
                boxOf.put(member, saved);
            }
            changed = true;
        }
        List<TankShape> loose = new ArrayList<>(ready);
        if (!released.isEmpty()) {
            Iterator<Map.Entry<NodePos, TankShape>> it = waiting.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<NodePos, TankShape> entry = it.next();
                if (released.contains(entry.getValue())) {
                    it.remove();
                    loose.add(boxOf.get(entry.getKey()));
                }
            }
            changed = true;
        }
        mergeFrom(loose);
        return changed;
    }

    /** Merges the given boxes with their neighbours, and the results again, until nothing merges. */
    private void mergeFrom(Collection<TankShape> start) {
        Deque<TankShape> queue = new ArrayDeque<>(start);
        while (!queue.isEmpty()) {
            TankShape box = queue.poll();
            if (!isCurrent(box) || isWaitingBox(box)) {
                continue;
            }
            for (NodePos probe : probes(box)) {
                TankShape other = boxOf.get(probe);
                if (other == null || other.equals(box) || isWaitingBox(other)) {
                    continue;
                }
                Optional<TankShape> merged = box.mergeWith(other);
                if (merged.isPresent() && rules.mayMerge(box, other)) {
                    for (NodePos member : merged.get().positions()) {
                        boxOf.put(member, merged.get());
                    }
                    queue.addFirst(merged.get());
                    break;
                }
            }
        }
    }

    private boolean isCurrent(TankShape box) {
        return box.equals(boxOf.get(new NodePos(box.minX(), box.minY(), box.minZ())));
    }

    private boolean isWaitingBox(TankShape box) {
        return box.volume() == 1 && waiting.containsKey(new NodePos(box.minX(), box.minY(), box.minZ()));
    }

    /** One position just outside each face; a box matching that face must contain it. Sides first, then up and down. */
    private static List<NodePos> probes(TankShape box) {
        return List.of(
                new NodePos(box.maxX() + 1, box.minY(), box.minZ()),
                new NodePos(box.minX() - 1, box.minY(), box.minZ()),
                new NodePos(box.minX(), box.minY(), box.maxZ() + 1),
                new NodePos(box.minX(), box.minY(), box.minZ() - 1),
                new NodePos(box.minX(), box.maxY() + 1, box.minZ()),
                new NodePos(box.minX(), box.minY() - 1, box.minZ()));
    }
}
