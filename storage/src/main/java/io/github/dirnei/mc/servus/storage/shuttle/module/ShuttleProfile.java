package io.github.dirnei.mc.servus.storage.shuttle.module;

import java.util.Optional;
import java.util.Set;

/**
 * What one shuttle can do, resolved from its tier, tool and upgrade counts
 * once when its garage slot changes, so the fleet never looks at items while
 * it ticks.
 *
 * @param modules   ids of the fitted modules
 * @param fieldOnly whether the shuttle serves fields only
 * @param stacks    how many stacks, and so how many jobs, it carries per trip
 * @param speed     blocks per tick between stops
 * @param handling  the factor its Handling upgrades put on stop and work times
 */
public record ShuttleProfile(Set<String> modules, boolean fieldOnly, int stacks, double speed, double handling, boolean liftless) {

    public ShuttleProfile {
        modules = Set.copyOf(modules);
    }

    /** The profile of a shuttle of that tier with this tool and these counts, by the given stats. */
    public static ShuttleProfile of(ShuttleTiers tiers, int tier, Optional<ModuleSpec> tool, int speed, int handling) {
        Set<String> ids = tool.map(spec -> Set.of(spec.id())).orElse(Set.of());
        boolean fieldOnly = tool.map(ModuleSpec::fieldOnly).orElse(false);
        boolean liftless = tool.map(ModuleSpec::wide).orElse(false);
        return new ShuttleProfile(ids, fieldOnly, tiers.tier(tier).stacks(), tiers.speed(tier, speed), tiers.factor(tier, handling), liftless);
    }

    /** A Mk1 shuttle without tool or upgrades by the default stats. */
    public static ShuttleProfile plain() {
        return of(ShuttleTiers.DEFAULT, 1, Optional.empty(), 0, 0);
    }
}
