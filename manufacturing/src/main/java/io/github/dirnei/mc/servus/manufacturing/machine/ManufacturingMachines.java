package io.github.dirnei.mc.servus.manufacturing.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.List;

/** Manufacturing's machines in the shared {@link MachineDefaults} table. */
public final class ManufacturingMachines {

    /** Pseudo recipe type of the research lab: one pack per {@code ticks_per_pack}. */
    public static final String RESEARCH = "servus:research";

    private static boolean registered;

    private ManufacturingMachines() {
    }

    /** Registers the defaults once; later calls do nothing. */
    public static synchronized void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        add("research_lab", "powered", List.of(RESEARCH), MachineStats.powered(1, 10, 4000));
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        MachineDefaults.register("servus_manufacturing:" + path, kind, recipeTypes, stats);
    }
}
