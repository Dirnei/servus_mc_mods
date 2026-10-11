package io.github.dirnei.mc.servus.storage.shuttle.module;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What one shuttle can do, resolved from its modules once when its garage
 * slot changes, so the fleet never looks at items while it ticks.
 *
 * @param modules   ids of the fitted modules
 * @param fieldOnly whether the shuttle serves fields only
 * @param stacks    how many stacks, and so how many jobs, it carries per trip
 */
public record ShuttleProfile(Set<String> modules, boolean fieldOnly, int stacks) {

    public static final ShuttleProfile PLAIN = new ShuttleProfile(Set.of(), false, 1);

    public ShuttleProfile {
        modules = Set.copyOf(modules);
    }

    /** The profile of a shuttle with these modules; a Cargo Hold carries cargoHoldStacks stacks, at least one. */
    public static ShuttleProfile of(Collection<ModuleSpec> fitted, int cargoHoldStacks) {
        Set<String> ids = new LinkedHashSet<>();
        boolean fieldOnly = false;
        boolean cargoHold = false;
        for (ModuleSpec spec : fitted) {
            ids.add(spec.id());
            fieldOnly |= spec.fieldOnly();
            cargoHold |= spec.cargoHold();
        }
        return new ShuttleProfile(ids, fieldOnly, cargoHold ? Math.max(1, cargoHoldStacks) : 1);
    }
}
