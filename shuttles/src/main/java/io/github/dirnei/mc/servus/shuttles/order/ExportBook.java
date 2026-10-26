package io.github.dirnei.mc.servus.shuttles.order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Which station and keep slot holds each export: at most one per export, and
 * at most one export per slot. Pure, no Minecraft imports.
 *
 * @param <K> the item key
 * @param <P> the station position
 */
public final class ExportBook<K, P> {

    /** The station and keep slot an export occupies. */
    public record Claim<P>(P station, int slot) {
    }

    private final Map<K, Claim<P>> claims = new LinkedHashMap<>();

    /** The station holding the export, empty when none does. */
    public Optional<Claim<P>> holder(K key) {
        return Optional.ofNullable(claims.get(key));
    }

    /**
     * Gives the export to the station's slot, taking it from a station that
     * held it. Refused when another export occupies that slot.
     *
     * @return whether the claim was made
     */
    public boolean claim(K key, P station, int slot) {
        for (Map.Entry<K, Claim<P>> entry : claims.entrySet()) {
            Claim<P> other = entry.getValue();
            if (!entry.getKey().equals(key) && other.slot() == slot && Objects.equals(other.station(), station)) {
                return false;
            }
        }
        claims.put(key, new Claim<>(station, slot));
        return true;
    }

    /** Frees a station's slot; returns the export that held it. */
    public Optional<K> releaseSlot(P station, int slot) {
        for (Map.Entry<K, Claim<P>> entry : claims.entrySet()) {
            if (entry.getValue().slot() == slot && Objects.equals(entry.getValue().station(), station)) {
                K key = entry.getKey();
                claims.remove(key);
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    /** Frees every slot of a station; returns the exports it held. */
    public List<K> releaseStation(P station) {
        List<K> released = new ArrayList<>();
        claims.entrySet().removeIf(entry -> {
            boolean held = Objects.equals(entry.getValue().station(), station);
            if (held) {
                released.add(entry.getKey());
            }
            return held;
        });
        return released;
    }

    /** Forgets the export's station, as when its threshold or its rule is removed; true when one held it. */
    public boolean clear(K key) {
        return claims.remove(key) != null;
    }

    /** The exports a station holds, by keep slot. */
    public Map<Integer, K> slotsOf(P station) {
        Map<Integer, K> slots = new TreeMap<>();
        claims.forEach((key, claim) -> {
            if (Objects.equals(claim.station(), station)) {
                slots.put(claim.slot(), key);
            }
        });
        return slots;
    }

    /** Every claim, in the order they were made. */
    public Map<K, Claim<P>> all() {
        return Collections.unmodifiableMap(claims);
    }
}
