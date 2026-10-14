package io.github.dirnei.mc.servus.alchemy.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.List;

/** Manufacturing's machines in the shared {@link MachineDefaults} table. */
public final class AlchemyMachineDefaults {

    private static boolean registered;

    private AlchemyMachineDefaults() {
    }

    /** Registers the defaults once; later calls do nothing. */
    public static synchronized void registerDefaults() {
        if (registered) {
            return;
        }
        registered = true;
        add("calciner", "burner", List.of("servus_manufacturing:calcination"), calciner(2, 64));
        add("coagulator", "crafter", List.of("servus_manufacturing:coagulation"), alchemy(4000));
        add("athanor", "crafter", List.of("servus_manufacturing:athanor"), alchemy(4000));
        add("purifier", "crafter", List.of("servus_manufacturing:purification"), alchemy(4000));
        add("gnome_hollow", "crafter", List.of(), alchemy(4000));
    }

    /**
     * The Calciner burns furnace fuel, a burn lasting the fuel's furnace time divided by {@code fuelDivisor}, keeps coal as
     * upkeep and holds {@code ash} Ash in its Ash Pan; its Sulfur and Mercury go straight into its tanks.
     */
    private static MachineStats calciner(int fuelDivisor, int ash) {
        return new MachineStats(1, 0, 0, fuelDivisor, 0, 1, 0, 0, 0, 0, MachineStats.DEFAULT_UPKEEP_ITEM, MachineStats.DEFAULT_UPKEEP_LEVEL, ash);
    }

    /** An unpowered alchemy machine: recipe time decides the cycle, each principle tank holds {@code tank} mB. */
    private static MachineStats alchemy(int tank) {
        return new MachineStats(1, 0, 0, 1, 0, 1, 0, 0, tank, 0, "", 0, 0);
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        MachineDefaults.register("servus_manufacturing:" + path, kind, recipeTypes, stats);
    }
}
