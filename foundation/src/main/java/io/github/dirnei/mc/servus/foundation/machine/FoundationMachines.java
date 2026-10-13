package io.github.dirnei.mc.servus.foundation.machine;

import java.util.List;

/** Foundation's machines in the shared {@link MachineDefaults} table. */
public final class FoundationMachines {

    private static boolean registered;

    private FoundationMachines() {
    }

    /** Registers the defaults once; later calls do nothing. */
    public static synchronized void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        add("coal_generator", "generator", MachineStats.generator(40, 16000, 4));
        add("burner_pump", "burner", MachineStats.burnerPump(20, 250, 4000, 2));
        add("electric_pump", "powered", MachineStats.poweredPump(20, 20, 8000, 500, 4000, 2));
        add("ender_anchor", "powered", MachineStats.powered(1, 10, 20000));
    }

    private static void add(String path, String kind, MachineStats stats) {
        MachineDefaults.register("servus_foundation:" + path, kind, List.of(), stats);
    }
}
