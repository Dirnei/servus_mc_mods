package io.github.dirnei.mc.servus.storage.shuttle;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lift car of one shaft: where it is, who rides it, who waits for it and
 * who crosses its decks. Floors are the y of a segment; the car stands at a
 * floor when its y equals that floor. One rider per trip, calls served in
 * order. Pure, no Minecraft imports.
 */
public final class LiftCar {

    /** A shuttle waiting at a floor for the car. */
    public record Call(int id, int floor) {
    }

    private int bottomY;
    private int topY;
    private double y;
    private Integer rider;
    private Integer riderTarget;
    private final ArrayDeque<Call> calls = new ArrayDeque<>();
    private final Map<Integer, Integer> crossers = new LinkedHashMap<>();
    private boolean moving;

    /** A new car stands at the lowest floor. */
    public LiftCar(int bottomY, int topY) {
        this.bottomY = bottomY;
        this.topY = topY;
        this.y = bottomY;
    }

    public double y() {
        return y;
    }

    public int bottomY() {
        return bottomY;
    }

    public int topY() {
        return topY;
    }

    /** The floor the car is heading for, or null when nothing needs it. */
    public Integer target() {
        if (rider != null) {
            return riderTarget;
        }
        Call first = calls.peekFirst();
        return first == null ? null : first.floor();
    }

    /** Whether the car moved on its last tick. */
    public boolean moving() {
        return moving;
    }

    public Integer rider() {
        return rider;
    }

    public boolean carrying() {
        return rider != null;
    }

    /** The waiting calls in the order they were made. */
    public List<Call> calls() {
        return new ArrayList<>(calls);
    }

    public boolean hasCalled(int id) {
        for (Call call : calls) {
            if (call.id() == id) {
                return true;
            }
        }
        return false;
    }

    /** The shuttle waits at that floor for the car. Calling again changes nothing. */
    public void call(int id, int floor) {
        if (hasCalled(id) || (rider != null && rider == id)) {
            return;
        }
        calls.addLast(new Call(id, floor));
    }

    /** Whether the car stands idle at the floor, empty, with the shuttle's call first in line. */
    public boolean canBoard(int id, int floor) {
        Call first = calls.peekFirst();
        return rider == null && first != null && first.id() == id && first.floor() == floor && y == floor;
    }

    /** The first caller drives onto the car; call only when {@link #canBoard} is true. */
    public void board(int id) {
        Call first = calls.peekFirst();
        if (first == null || first.id() != id || rider != null) {
            throw new IllegalStateException("shuttle " + id + " cannot board");
        }
        calls.pollFirst();
        rider = id;
        riderTarget = null;
    }

    /** The rider wants to be taken to that floor. */
    public void send(int id, int floor) {
        if (rider != null && rider == id) {
            riderTarget = clamp(floor);
        }
    }

    /** Whether the rider stands at the floor it was sent to. */
    public boolean arrivedFor(int id, int floor) {
        return rider != null && rider == id && riderTarget != null && riderTarget == floor && y == floor;
    }

    /** The rider drove off; the car serves the next call. */
    public void leave(int id) {
        if (rider != null && rider == id) {
            rider = null;
            riderTarget = null;
        }
    }

    /** Whether a shuttle may drive across the floor: the deck there is down, or the car stands there empty. */
    public boolean canCross(int floor) {
        return Flaps.angle(y, floor) == 0 || (y == floor && rider == null && !moving);
    }

    public void beginCross(int id, int floor) {
        crossers.put(id, floor);
    }

    public void endCross(int id) {
        crossers.remove(id);
    }

    public boolean isCrossing(int id) {
        return crossers.containsKey(id);
    }

    /** Moves the car towards its target, unless the next step would fold a deck a shuttle is crossing. */
    public void tick(double speed) {
        moving = false;
        Integer target = target();
        if (target == null || y == target) {
            return;
        }
        double left = target - y;
        double next = Math.abs(left) <= speed + 1e-9 ? target : y + Math.signum(left) * speed;
        for (int floor : crossers.values()) {
            if (Flaps.angle(next, floor) > 0) {
                return;
            }
        }
        y = next;
        moving = true;
    }

    /** The shaft grew or shrank: the car stays where it is, inside the new bounds. */
    public void resize(int bottomY, int topY) {
        this.bottomY = bottomY;
        this.topY = topY;
        y = Math.max(bottomY, Math.min(topY, y));
        if (riderTarget != null) {
            riderTarget = clamp(riderTarget);
        }
    }

    /** Forgets a shuttle: its call, its ride or its crossing. */
    public void remove(int id) {
        calls.removeIf(call -> call.id() == id);
        if (rider != null && rider == id) {
            rider = null;
            riderTarget = null;
        }
        crossers.remove(id);
    }

    private int clamp(int floor) {
        return Math.max(bottomY, Math.min(topY, floor));
    }
}
