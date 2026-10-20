package io.github.dirnei.mc.servus.shuttles.shuttle.module;

import java.util.Set;

/**
 * What a job asks of the shuttle that takes it.
 *
 * @param modules ids of the modules the shuttle must have
 * @param field   whether the job is field or tool work; other jobs never go to field only shuttles
 */
public record JobNeeds(Set<String> modules, boolean field) {

    /** Warehouse picks and put-aways: any shuttle that is not field only. */
    public static final JobNeeds WAREHOUSE = new JobNeeds(Set.of(), false);

    /** Field jobs: a shuttle with a Harvester. */
    public static final JobNeeds HARVESTER = new JobNeeds(Set.of(ModuleSpec.HARVESTER), true);

    /** Fertilize runs: a shuttle with a Spreader. */
    public static final JobNeeds SPREADER = new JobNeeds(Set.of(ModuleSpec.SPREADER), true);

    /** Tanker runs: a shuttle with a Tanker, which takes no other jobs. */
    public static final JobNeeds TANKER = new JobNeeds(Set.of(ModuleSpec.TANKER), true);

    public JobNeeds {
        modules = Set.copyOf(modules);
    }

    public boolean fits(ShuttleProfile profile) {
        return profile.modules().containsAll(modules) && (field || !profile.fieldOnly());
    }
}
