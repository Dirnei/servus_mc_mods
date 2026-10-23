package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import io.github.dirnei.mc.servus.shuttles.shuttle.Shafts.Shaft;

/**
 * Travel time weights in ticks for a step over rails and lift shafts. Pure Java.
 *
 * @param railBlock      one horizontal block
 * @param corner         extra for a step that turns
 * @param liftBlock      one block ridden in a lift car
 * @param handover       one ride's handovers, getting on and off
 * @param stop           one stop at a block
 * @param liftWaitPerJob the wait for every call or planned ride ahead at a shaft
 */
public record TravelWeights(double railBlock, double corner, double liftBlock, double handover, double stop, double liftWaitPerJob) {

    public static final TravelWeights DEFAULT = new TravelWeights(7, 5, 10, 20, 14, 50);

    /** What a step needs to know about lifts: the shaft of a node and the wait for its car at a floor. */
    public interface Lifts {
        Shaft shaftOf(NodePos node);

        /** Ticks until the shaft's car can take a shuttle at that floor. */
        double waitAt(Shaft shaft, int floorY);
    }

    /** Lifts that never wait, for a graph without shafts or tests. */
    public static final Lifts NO_WAIT = new Lifts() {
        @Override
        public Shaft shaftOf(NodePos node) {
            return null;
        }

        @Override
        public double waitAt(Shaft shaft, int floorY) {
            return 0;
        }
    };

    /** The wait at a floor: the car's way there plus every call or ride ahead of the shuttle. */
    public double liftWait(double carY, int floorY, int jobsAhead) {
        return Math.abs(carY - floorY) * liftBlock + Math.max(0, jobsAhead) * liftWaitPerJob;
    }

    /** The cost of stepping from {@code from} to {@code to}, {@code previous} being the node before, or null. */
    public double step(NodePos previous, NodePos from, NodePos to, Lifts lifts) {
        if (vertical(from, to)) {
            double cost = liftBlock;
            if (previous == null || !vertical(previous, from)) {
                cost += handover;
                Shaft shaft = lifts.shaftOf(from);
                if (shaft != null) {
                    cost += lifts.waitAt(shaft, from.y());
                }
            }
            return cost;
        }
        double cost = railBlock;
        if (previous != null && !vertical(previous, from) && turns(previous, from, to)) {
            cost += corner;
        }
        return cost;
    }

    private static boolean vertical(NodePos a, NodePos b) {
        return a.x() == b.x() && a.z() == b.z() && a.y() != b.y();
    }

    private static boolean turns(NodePos a, NodePos b, NodePos c) {
        return b.x() - a.x() != c.x() - b.x() || b.z() - a.z() != c.z() - b.z();
    }
}
