package io.github.dirnei.mc.servus.shuttles.shuttle;

import io.github.dirnei.mc.servus.foundation.graph.NodePos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * How shuttles share rails, worked out once per tick from where they stand:
 * shuttles in the same lane keep a gap to the one ahead, merging shuttles go
 * in turn, opposite headings pass, and shuttles leaving a garage wait for a
 * clear rail. Pure Java.
 */
public final class ShuttleTraffic {

    /** No direction: before the first block a shuttle drove, or after the last one of its path. */
    public static final int NONE = -1;
    private static final double TIE = 1e-9;

    private ShuttleTraffic() {
    }

    /** The direction of a step between two nodes, the signs of x, y and z packed into one int. */
    public static int direction(NodePos from, NodePos to) {
        return (Integer.signum(to.x() - from.x()) + 1) * 9 + (Integer.signum(to.y() - from.y()) + 1) * 3
                + Integer.signum(to.z() - from.z()) + 1;
    }

    /** The opposite direction, {@link #NONE} for none. */
    static int reverse(int direction) {
        return direction == NONE ? NONE : 26 - direction;
    }

    /** The node one step against the direction, so that a step from it in that direction leads to the node. */
    static NodePos behind(NodePos node, int direction) {
        return new NodePos(node.x() - (direction / 9 - 1), node.y() - (direction / 3 % 3 - 1), node.z() - (direction % 3 - 1));
    }

    /**
     * One shuttle as traffic sees it.
     *
     * @param order       who goes first on a tie, the lower first
     * @param lastHeading the direction of the last block driven before this path, {@link #NONE} when unknown
     * @param inGarage    still inside its garage at the first node of its path
     * @param obstacle    whether others keep clear of it; false while it rides or is about to ride a lift car
     * @param moving      whether it drives this tick; false while it stands for a stop, a gate or anything else
     * @param speed       blocks it drove last tick
     * @param accel       its braking rate in blocks per tick per tick
     * @param stopTicks   ticks left of the stop it stands in, 0 when none
     */
    public record Car(int id, long order, List<NodePos> path, int segment, double progress, int lastHeading, boolean inGarage,
                      boolean obstacle, boolean moving, double speed, double accel, int stopTicks) {

        double position() {
            return segment + progress;
        }

        int last() {
            return path.size() - 1;
        }

        /**
         * The direction of the step into path node k; at the first node the
         * last heading, unless the path turns back there, which leaves the
         * car facing the way it came.
         */
        int in(int k) {
            if (k > 0) {
                return direction(path.get(k - 1), path.get(k));
            }
            return lastHeading == reverse(out(0)) ? NONE : lastHeading;
        }

        /** The direction of the step out of path node k. */
        int out(int k) {
            return k < last() ? direction(path.get(k), path.get(k + 1)) : NONE;
        }

        /** Blocks it needs to stand still from its speed. */
        double brake() {
            return accel <= 0 ? 0 : speed * speed / (2 * accel);
        }
    }

    /**
     * What holds a car back this tick.
     *
     * @param room        blocks it may still drive before it is the gap behind its leader, infinite without one
     * @param leader      the id of that leader, -1 without one
     * @param leaderSpeed how fast the leader drove last tick, 0 while it stands
     */
    public record Limit(double room, int leader, double leaderSpeed) {

        public static final Limit FREE = new Limit(Double.POSITIVE_INFINITY, -1, 0);

        public boolean led() {
            return leader >= 0;
        }
    }

    /** A step of a way, from one node to the next. */
    public record Step(NodePos from, NodePos to) {
    }

    private record Entry(Car car, double distance, int in, int out) {
    }

    /**
     * The limit of every car: the nearest car ahead in its lane, or the car it
     * merges behind, within the window ahead of it. Cars look no further than
     * the start of their next lift ride; the car takes riders one at a time.
     *
     * @param gap    the distance from centre to centre a car keeps to its leader
     * @param window blocks ahead a car looks for leaders
     */
    public static Map<Integer, Limit> limits(Collection<Car> cars, double gap, double window) {
        Map<NodePos, List<Entry>> entries = entries(cars, window);
        Map<Integer, Limit> limits = new LinkedHashMap<>();
        for (Car car : cars) {
            limits.put(car.id(), limitOf(car, entries, gap, window));
        }
        return limits;
    }

    /** Every obstacle car at every node from the one it is on to the window ahead, with its distance and headings there. */
    private static Map<NodePos, List<Entry>> entries(Collection<Car> cars, double window) {
        Map<NodePos, List<Entry>> entries = new HashMap<>();
        for (Car car : cars) {
            if (!car.obstacle()) {
                continue;
            }
            for (int k = Math.min(car.segment(), car.last()); k <= car.last(); k++) {
                double distance = k - car.position();
                if (distance > window) {
                    break;
                }
                entries.computeIfAbsent(car.path().get(k), node -> new ArrayList<>()).add(new Entry(car, distance, car.in(k), car.out(k)));
                if (vertical(car.out(k))) {
                    break;
                }
            }
        }
        return entries;
    }

    private static Limit limitOf(Car car, Map<NodePos, List<Entry>> entries, double gap, double window) {
        double room = Double.POSITIVE_INFINITY;
        Car leader = null;
        for (int k = Math.min(car.segment(), car.last()); k <= car.last(); k++) {
            double distance = k - car.position();
            if (distance > window) {
                break;
            }
            List<Entry> here = entries.get(car.path().get(k));
            if (here == null) {
                continue;
            }
            int in = car.in(k);
            int out = car.out(k);
            for (Entry entry : here) {
                Car other = entry.car();
                if (other.id() == car.id() || !sharesLane(in, out, entry.in(), entry.out())
                        || !goesFirst(other, entry.distance(), car, distance, gap)) {
                    continue;
                }
                double left = distance - entry.distance() - gap;
                if (left < room) {
                    room = left;
                    leader = other;
                }
            }
            if (vertical(out)) {
                break;
            }
        }
        return leader == null ? Limit.FREE : new Limit(room, leader.id(), leader.moving() ? leader.speed() : 0);
    }

    /** Whether a direction goes up or down, which only a lift car does. */
    static boolean vertical(int direction) {
        return direction != NONE && direction / 3 % 3 != 1;
    }

    /** Whether two cars drive into or out of a node in a common direction. */
    static boolean sharesLane(int in, int out, int otherIn, int otherOut) {
        return same(in, otherIn) || same(in, otherOut) || same(out, otherIn) || same(out, otherOut);
    }

    private static boolean same(int a, int b) {
        return a != NONE && a == b;
    }

    /**
     * Whether {@code other}, {@code otherDistance} from a node they share a
     * lane at, goes before {@code car}, {@code distance} from it. Out on the
     * rails the nearer goes first, on a tie the lower order. A car inside its
     * garage is nothing to cars on the rails, and to other cars inside their
     * garages it is a car standing at its garage's rail; it goes after a car on
     * the rails that stands on or ahead of the node or could not stop before it.
     */
    static boolean goesFirst(Car other, double otherDistance, Car car, double distance, double gap) {
        if (other.inGarage()) {
            return car.inGarage() && nearerOrLower(other, otherDistance, car, distance);
        }
        if (car.inGarage()) {
            if (otherDistance <= distance) {
                return true;
            }
            return other.moving() && otherDistance - distance - gap < other.brake() + other.speed();
        }
        return nearerOrLower(other, otherDistance, car, distance);
    }

    private static boolean nearerOrLower(Car other, double otherDistance, Car car, double distance) {
        if (Math.abs(otherDistance - distance) > TIE) {
            return otherDistance < distance;
        }
        return other.order() < car.order();
    }

    /** The rings of cars that wait for each other in a closed loop, from each car's binding leader; each ring in leader order. */
    public static List<List<Integer>> rings(Map<Integer, Integer> leaderOf) {
        List<List<Integer>> rings = new ArrayList<>();
        Set<Integer> done = new HashSet<>();
        for (Integer start : leaderOf.keySet()) {
            List<Integer> trail = new ArrayList<>();
            Map<Integer, Integer> index = new HashMap<>();
            Integer at = start;
            while (at != null && !done.contains(at) && !index.containsKey(at)) {
                index.put(at, trail.size());
                trail.add(at);
                at = leaderOf.get(at);
            }
            if (at != null && index.containsKey(at)) {
                rings.add(List.copyOf(trail.subList(index.get(at), trail.size())));
            }
            done.addAll(trail);
        }
        return rings;
    }

    /** Whether some car on the chain of leaders ahead of {@code start} passes the test. */
    public static boolean chainReaches(Map<Integer, Integer> leaderOf, int start, IntPredicate test) {
        Set<Integer> seen = new HashSet<>();
        Integer at = leaderOf.get(start);
        while (at != null && seen.add(at)) {
            if (test.test(at)) {
                return true;
            }
            at = leaderOf.get(at);
        }
        return false;
    }

    /**
     * The traffic on each step: a car between two nodes loads that step with
     * {@code perShuttle}, a car standing on a node loads the step into it with
     * {@code perShuttle} and its stop ticks left. Cars in their garage and
     * cars that are no obstacle load nothing.
     */
    public static Map<Step, Double> stepLoads(Collection<Car> cars, double perShuttle) {
        Map<Step, Double> loads = new HashMap<>();
        for (Car car : cars) {
            if (car.inGarage() || !car.obstacle()) {
                continue;
            }
            int k = Math.min(car.segment(), car.last());
            NodePos at = car.path().get(k);
            if (car.progress() > 0 && k < car.last()) {
                loads.merge(new Step(at, car.path().get(k + 1)), perShuttle, Double::sum);
                continue;
            }
            int in = car.in(k);
            if (in != NONE) {
                loads.merge(new Step(k > 0 ? car.path().get(k - 1) : behind(at, in), at), perShuttle + car.stopTicks(), Double::sum);
            }
        }
        return loads;
    }
}
