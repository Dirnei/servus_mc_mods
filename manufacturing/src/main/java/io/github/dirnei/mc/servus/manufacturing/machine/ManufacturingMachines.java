package io.github.dirnei.mc.servus.manufacturing.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.List;

/** Manufacturing's machines in the shared {@link MachineDefaults} table. */
public final class ManufacturingMachines {

    private static boolean registered;

    private ManufacturingMachines() {
    }

    /** Registers the defaults once; later calls do nothing. */
    public static synchronized void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        add("calciner", "powered", List.of("servus_manufacturing:calcination"), alchemy(20, 8000, 4000));
        add("coagulator", "powered", List.of("servus_manufacturing:coagulation"), alchemy(20, 8000, 4000));
        add("athanor", "crafter", List.of("servus_manufacturing:athanor"), alchemy(0, 0, 4000));
        add("gnome_hollow", "crafter", List.of(), alchemy(0, 0, 4000));
    }

    /** A powered alchemy machine: recipe time decides the cycle, one principle tank of {@code tank} mB each. */
    private static MachineStats alchemy(int energyPerTick, int energyCapacity, int tank) {
        return new MachineStats(1, energyPerTick, energyCapacity, 1, 0, 1, 0, 0, tank, 0, "", 0);
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        MachineDefaults.register("servus_manufacturing:" + path, kind, recipeTypes, stats);
    }
}
