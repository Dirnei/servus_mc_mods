package io.github.dirnei.mc.servus.foundation.logistics.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

/**
 * Decides which targets of a channel receive how much of an offered amount.
 * Targets are grouped per channel into priority groups, highest first. Within
 * a group the offer goes round robin from a cursor that only moves when a
 * delivery is recorded, so a target that rejected everything does not lose
 * its turn. Only when a group cannot take the whole amount does the rest move
 * on to the next lower priority. Pure Java, no Minecraft imports.
 *
 * @param <T> the target type, compared by {@link Object#equals}
 */
public final class RoundRobinPlan<T> {

    /** A target on a channel with its priority. */
    public record Target<T>(int channel, int priority, T value) {
    }

    /** How much of an offer one target should get. */
    public record Offer<T>(T target, int amount) {
    }

    private record Key(int channel, int priority) {
    }

    private static final class Group<T> {
        final int priority;
        final List<T> targets = new ArrayList<>();

        Group(int priority) {
            this.priority = priority;
        }
    }

    /** Per channel, the priority groups highest first. */
    private final Map<Integer, List<Group<T>>> channels = new HashMap<>();
    /** Round robin cursor per channel and priority; survives rebuilds. */
    private final Map<Key, Integer> cursors = new HashMap<>();

    /** Replaces all targets. Cursors are kept, so rebuilding after a configuration change stays fair. */
    public void rebuild(Collection<Target<T>> targets) {
        channels.clear();
        Map<Integer, Map<Integer, Group<T>>> byChannel = new HashMap<>();
        for (Target<T> target : targets) {
            byChannel.computeIfAbsent(target.channel(), c -> new HashMap<>())
                    .computeIfAbsent(target.priority(), p -> new Group<>(p))
                    .targets.add(target.value());
        }
        for (Map.Entry<Integer, Map<Integer, Group<T>>> entry : byChannel.entrySet()) {
            List<Group<T>> groups = new ArrayList<>(entry.getValue().values());
            groups.sort((a, b) -> Integer.compare(b.priority, a.priority));
            channels.put(entry.getKey(), groups);
        }
    }

    public void clear() {
        channels.clear();
        cursors.clear();
    }

    /** Whether the channel has any target. */
    public boolean hasTargets(int channel) {
        List<Group<T>> groups = channels.get(channel);
        return groups != null && !groups.isEmpty();
    }

    /**
     * Plans how {@code amount} is split over the channel's targets. Does not
     * move any cursor; call {@link #delivered} for what actually arrived.
     *
     * @param capacity how much a target can take right now; zero or less skips it
     * @return offers in delivery order, possibly summing to less than {@code amount}
     */
    public List<Offer<T>> plan(int channel, int amount, ToIntFunction<T> capacity) {
        List<Offer<T>> offers = new ArrayList<>();
        List<Group<T>> groups = channels.get(channel);
        if (groups == null || amount <= 0) {
            return offers;
        }
        int remaining = amount;
        for (Group<T> group : groups) {
            int size = group.targets.size();
            int cursor = cursors.getOrDefault(new Key(channel, group.priority), 0) % Math.max(1, size);
            for (int i = 0; i < size && remaining > 0; i++) {
                T target = group.targets.get((cursor + i) % size);
                int take = Math.min(remaining, capacity.applyAsInt(target));
                if (take <= 0) {
                    continue;
                }
                offers.add(new Offer<>(target, take));
                remaining -= take;
            }
            if (remaining <= 0) {
                break;
            }
        }
        return offers;
    }

    /** Records that the target on the channel received something: its group's cursor moves past it. */
    public void delivered(int channel, T target) {
        List<Group<T>> groups = channels.get(channel);
        if (groups == null) {
            return;
        }
        for (Group<T> group : groups) {
            int index = group.targets.indexOf(target);
            if (index >= 0) {
                cursors.put(new Key(channel, group.priority), (index + 1) % group.targets.size());
                return;
            }
        }
    }
}
