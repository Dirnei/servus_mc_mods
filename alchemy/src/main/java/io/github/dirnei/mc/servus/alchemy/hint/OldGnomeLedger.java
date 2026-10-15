package io.github.dirnei.mc.servus.alchemy.hint;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Where each team's old gnome is: asleep, awake in his Ancient Hollow, or found. Pure Java; positions are packed block
 * positions and dimensions their ids, so the world data only stores what this holds.
 */
public final class OldGnomeLedger {

    /** How far a team has come with its old gnome. */
    public enum State {
        /** Not woken yet, or his Hollow was lost before he was found. */
        NONE,
        /** His Ancient Hollow stands and gives hints. */
        AWAKE,
        /** The team found him; he never wakes again. */
        FOUND
    }

    /**
     * One team's old gnome.
     *
     * @param state     how far the team has come
     * @param pos       packed position of the Ancient Hollow, meaningful while awake or found
     * @param dimension dimension id of the Ancient Hollow, empty while none
     */
    public record Entry(State state, long pos, String dimension) {

        static final Entry NONE = new Entry(State.NONE, 0, "");
    }

    private final Map<UUID, Entry> teams = new HashMap<>();

    /** The team's entry; {@link Entry#NONE} for a team that never woke him. */
    public Entry entry(UUID team) {
        return teams.getOrDefault(team, Entry.NONE);
    }

    /** Whether a break may wake the team's old gnome: only while he sleeps. */
    public boolean mayWake(UUID team) {
        return entry(team).state() == State.NONE;
    }

    /** Wakes him in his Ancient Hollow; returns false when he is already awake or found. */
    public boolean wake(UUID team, long pos, String dimension) {
        if (!mayWake(team)) {
            return false;
        }
        teams.put(team, new Entry(State.AWAKE, pos, dimension));
        return true;
    }

    /** Marks him found; returns false unless he was awake. */
    public boolean found(UUID team) {
        Entry entry = entry(team);
        if (entry.state() != State.AWAKE) {
            return false;
        }
        teams.put(team, new Entry(State.FOUND, entry.pos(), entry.dimension()));
        return true;
    }

    /** His Hollow at {@code pos} is gone; before he was found he may wake again elsewhere. Returns whether that applied. */
    public boolean lost(UUID team, long pos) {
        Entry entry = entry(team);
        if (entry.state() != State.AWAKE || entry.pos() != pos) {
            return false;
        }
        teams.remove(team);
        return true;
    }

    /** The awake gnome's entry of the team, empty when he sleeps or is found. */
    public Optional<Entry> awake(UUID team) {
        Entry entry = entry(team);
        return entry.state() == State.AWAKE ? Optional.of(entry) : Optional.empty();
    }

    /** Puts a saved entry back. */
    public void restore(UUID team, Entry entry) {
        if (entry.state() != State.NONE) {
            teams.put(team, entry);
        }
    }

    /** Every team with a woken or found gnome. */
    public Map<UUID, Entry> entries() {
        return Map.copyOf(teams);
    }
}
