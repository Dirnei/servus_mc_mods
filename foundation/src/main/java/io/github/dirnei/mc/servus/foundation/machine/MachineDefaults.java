package io.github.dirnei.mc.servus.foundation.machine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Code defaults of every Servus machine, the values a machine uses for fields its datapack file leaves out.
 * Each mod registers its own machines from its entrypoint; one table so the machines and the game data
 * export read the same numbers. {@link #all()} keeps registration order.
 */
public final class MachineDefaults {

    /**
     * @param id          machine id, also the name of its stats file
     * @param kind        burner, powered, crafter or generator
     * @param recipeTypes recipe types the machine runs, empty for machines without recipes
     * @param stats       the defaults
     */
    public record MachineInfo(String id, String kind, List<String> recipeTypes, MachineStats stats) {
    }

    private static final Map<String, MachineInfo> MACHINES = new LinkedHashMap<>();

    private MachineDefaults() {
    }

    /** Registers a machine's defaults. Registering the same id twice is refused. */
    public static synchronized void register(String id, String kind, List<String> recipeTypes, MachineStats stats) {
        if (MACHINES.containsKey(id)) {
            throw new IllegalStateException("Machine defaults already registered for " + id);
        }
        MACHINES.put(id, new MachineInfo(id, kind, List.copyOf(recipeTypes), stats));
    }

    /** Every machine, in registration order. */
    public static Map<String, MachineInfo> all() {
        return Collections.unmodifiableMap(MACHINES);
    }

    public static MachineInfo info(String id) {
        MachineInfo info = MACHINES.get(id);
        if (info == null) {
            throw new NoSuchElementException("No machine defaults for " + id);
        }
        return info;
    }

    public static MachineStats stats(String id) {
        return info(id).stats();
    }
}
