package io.github.dirnei.mc.servus.foundation.logistics.channel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Every team's duct channels with the id counter. Ids start at 1 and are
 * never reused; 0 is the {@link Channel#DEFAULT Default} channel, which is
 * never stored, edited or deleted. Names are trimmed, 1 to {@link #MAX_NAME}
 * characters and unique within a team ignoring case. Pure Java.
 */
public final class ChannelList {

    public static final int DEFAULT_ID = 0;
    public static final int MAX_NAME = 24;

    /** Why a name is refused. */
    public enum NameProblem {
        EMPTY,
        TOO_LONG,
        TAKEN
    }

    private static final Comparator<Channel> BY_NAME = Comparator
            .comparing((Channel channel) -> !channel.isDefault())
            .thenComparing(channel -> channel.name().toLowerCase(Locale.ROOT))
            .thenComparingInt(Channel::id);

    private final Map<Integer, Channel> entries = new LinkedHashMap<>();
    private int nextId;

    public ChannelList() {
        this(List.of(), DEFAULT_ID + 1);
    }

    /** A list holding {@code channels}; the counter never drops to or below an id already taken. */
    public ChannelList(Collection<Channel> channels, int nextId) {
        int next = Math.max(DEFAULT_ID + 1, nextId);
        for (Channel channel : channels) {
            if (!channel.isDefault()) {
                entries.put(channel.id(), channel);
                next = Math.max(next, channel.id() + 1);
            }
        }
        this.nextId = next;
    }

    /** The id the next created channel gets. */
    public int nextId() {
        return nextId;
    }

    /** Every stored channel of every team, Default not included. */
    public Collection<Channel> all() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /** The channel with that id, {@link Channel#DEFAULT} for 0, null when it does not exist. */
    public Channel get(int id) {
        return id == DEFAULT_ID ? Channel.DEFAULT : entries.get(id);
    }

    public boolean exists(int id) {
        return get(id) != null;
    }

    /** The id itself when the channel exists, else Default's. */
    public int resolve(int id) {
        return exists(id) ? id : DEFAULT_ID;
    }

    /** The team's channels in creation order. */
    public List<Channel> forTeam(UUID team) {
        List<Channel> list = new ArrayList<>();
        for (Channel channel : entries.values()) {
            if (channel.team().equals(team)) {
                list.add(channel);
            }
        }
        return list;
    }

    /** The trimmed form a name is checked and stored in. */
    public static String clean(String name) {
        return name == null ? "" : name.strip();
    }

    /**
     * Why the team may not use {@code name}, or empty when it may. The channel
     * {@code exceptId} is skipped, so an edit may keep its own name.
     */
    public Optional<NameProblem> nameProblem(UUID team, String name, int exceptId) {
        String cleaned = clean(name);
        if (cleaned.isEmpty()) {
            return Optional.of(NameProblem.EMPTY);
        }
        if (cleaned.length() > MAX_NAME) {
            return Optional.of(NameProblem.TOO_LONG);
        }
        for (Channel channel : entries.values()) {
            if (channel.id() != exceptId && channel.team().equals(team) && channel.name().equalsIgnoreCase(cleaned)) {
                return Optional.of(NameProblem.TAKEN);
            }
        }
        return Optional.empty();
    }

    /** Creates a channel for the team; empty when the name is refused or the colour is out of range. */
    public Optional<Channel> create(UUID team, String name, int colour, String icon) {
        Objects.requireNonNull(team, "team");
        if (nameProblem(team, name, DEFAULT_ID).isPresent() || !ChannelColours.valid(colour)) {
            return Optional.empty();
        }
        Channel channel = new Channel(nextId++, team, clean(name), colour, blankToNull(icon));
        entries.put(channel.id(), channel);
        return Optional.of(channel);
    }

    /** Changes name, colour and icon, keeping id and team; empty when refused. Default is never edited. */
    public Optional<Channel> edit(int id, String name, int colour, String icon) {
        Channel old = entries.get(id);
        if (old == null || nameProblem(old.team(), name, id).isPresent() || !ChannelColours.valid(colour)) {
            return Optional.empty();
        }
        Channel channel = new Channel(id, old.team(), clean(name), colour, blankToNull(icon));
        entries.put(id, channel);
        return Optional.of(channel);
    }

    /** Removes a channel; false when it does not exist or is Default. */
    public boolean delete(int id) {
        return entries.remove(id) != null;
    }

    /**
     * Whether a channel may be deleted: never Default; otherwise when no loaded
     * side uses it, or its only loaded use is the side being configured.
     */
    public static boolean deletable(int id, int loadedUses, int usesOnOpenSide) {
        return id != DEFAULT_ID && (loadedUses == 0 || loadedUses == 1 && usesOnOpenSide == 1);
    }

    /**
     * The channels to list: Default first, then those of {@code channels}
     * whose name contains the query (case insensitive) and, when
     * {@code usedIds} is not null, whose id is in it, sorted by name.
     */
    public static List<Channel> matching(Collection<Channel> channels, String query, Set<Integer> usedIds) {
        String needle = clean(query).toLowerCase(Locale.ROOT);
        List<Channel> found = new ArrayList<>();
        found.add(Channel.DEFAULT);
        for (Channel channel : channels) {
            if (channel.isDefault() || usedIds != null && !usedIds.contains(channel.id())) {
                continue;
            }
            if (needle.isEmpty() || channel.name().toLowerCase(Locale.ROOT).contains(needle)) {
                found.add(channel);
            }
        }
        found.sort(BY_NAME);
        return found;
    }

    private static String blankToNull(String icon) {
        return icon == null || icon.isBlank() ? null : icon;
    }
}
