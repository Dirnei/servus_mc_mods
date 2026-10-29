package io.github.dirnei.mc.servus.shuttles.zone;

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
import java.util.UUID;

/**
 * Every team's warehouse zones with the id counter. Ids start at 1 and are
 * never reused; 0 is "no zone". Names are trimmed, 1 to {@link #MAX_NAME}
 * characters and unique within a team ignoring case. Pure Java: the filter
 * type is left open so the list needs no Minecraft classes.
 *
 * @param <F> what a zone takes
 */
public final class ZoneList<F> {

    public static final int NONE = 0;
    public static final int MAX_NAME = 24;
    public static final int COLOURS = 16;

    /** Why a name is refused. */
    public enum NameProblem {
        EMPTY,
        TOO_LONG,
        TAKEN
    }

    /**
     * One zone.
     *
     * @param id     never 0
     * @param team   the team that owns it
     * @param name   trimmed, 1 to {@link #MAX_NAME} characters
     * @param colour a dye colour index from 0 to 15
     * @param filter what the zone takes
     */
    public record Zone<F>(int id, UUID team, String name, int colour, F filter) {
    }

    private final Map<Integer, Zone<F>> entries = new LinkedHashMap<>();
    private int nextId;

    public ZoneList() {
        this(List.of(), NONE + 1);
    }

    /** A list holding {@code zones}; the counter never drops to or below an id already taken. */
    public ZoneList(Collection<Zone<F>> zones, int nextId) {
        int next = Math.max(NONE + 1, nextId);
        for (Zone<F> zone : zones) {
            if (zone.id() != NONE) {
                entries.put(zone.id(), zone);
                next = Math.max(next, zone.id() + 1);
            }
        }
        this.nextId = next;
    }

    /** The id the next created zone gets. */
    public int nextId() {
        return nextId;
    }

    /** Every zone of every team. */
    public Collection<Zone<F>> all() {
        return Collections.unmodifiableCollection(entries.values());
    }

    /** The zone with that id, null for 0 and unknown ids. */
    public Zone<F> get(int id) {
        return entries.get(id);
    }

    /** The id itself when the zone exists, else {@link #NONE}: a deleted zone reads as no zone. */
    public int resolve(int id) {
        return entries.containsKey(id) ? id : NONE;
    }

    /** The team's zones sorted by name. */
    public List<Zone<F>> forTeam(UUID team) {
        return matching(team, "");
    }

    /** The team's zones whose name contains the query, ignoring case, sorted by name. */
    public List<Zone<F>> matching(UUID team, String query) {
        String needle = clean(query).toLowerCase(Locale.ROOT);
        List<Zone<F>> found = new ArrayList<>();
        for (Zone<F> zone : entries.values()) {
            if (zone.team().equals(team) && (needle.isEmpty() || zone.name().toLowerCase(Locale.ROOT).contains(needle))) {
                found.add(zone);
            }
        }
        found.sort(Comparator.comparing((Zone<F> zone) -> zone.name().toLowerCase(Locale.ROOT)).thenComparingInt(Zone::id));
        return found;
    }

    /** The trimmed form a name is checked and stored in. */
    public static String clean(String name) {
        return name == null ? "" : name.strip();
    }

    public static boolean validColour(int colour) {
        return colour >= 0 && colour < COLOURS;
    }

    /**
     * Why the team may not use {@code name}, or empty when it may. The zone
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
        for (Zone<F> zone : entries.values()) {
            if (zone.id() != exceptId && zone.team().equals(team) && zone.name().equalsIgnoreCase(cleaned)) {
                return Optional.of(NameProblem.TAKEN);
            }
        }
        return Optional.empty();
    }

    /** Creates a zone for the team; empty when the name is refused or the colour is out of range. */
    public Optional<Zone<F>> create(UUID team, String name, int colour, F filter) {
        Objects.requireNonNull(team, "team");
        if (nameProblem(team, name, NONE).isPresent() || !validColour(colour)) {
            return Optional.empty();
        }
        Zone<F> zone = new Zone<>(nextId++, team, clean(name), colour, filter);
        entries.put(zone.id(), zone);
        return Optional.of(zone);
    }

    /** Changes name, colour and filter, keeping id and team; empty when refused. */
    public Optional<Zone<F>> edit(int id, String name, int colour, F filter) {
        Zone<F> old = entries.get(id);
        if (old == null || nameProblem(old.team(), name, id).isPresent() || !validColour(colour)) {
            return Optional.empty();
        }
        Zone<F> zone = new Zone<>(id, old.team(), clean(name), colour, filter);
        entries.put(id, zone);
        return Optional.of(zone);
    }

    /** Removes a zone; false when it does not exist. */
    public boolean delete(int id) {
        return entries.remove(id) != null;
    }
}
