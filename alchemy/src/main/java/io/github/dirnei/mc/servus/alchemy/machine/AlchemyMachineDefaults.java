package io.github.dirnei.mc.servus.alchemy.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.List;

/** Alchemy's machines in the shared {@link MachineDefaults} table. */
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
        add("calciner", "burner", List.of("servus_alchemy:calcination"), calciner(2, 64));
        add("coagulator", "crafter", List.of("servus_alchemy:coagulation"), alchemy(4000));
        add("athanor", "crafter", List.of("servus_alchemy:athanor"), alchemy(4000));
        add("purifier", "crafter", List.of("servus_alchemy:purification"), alchemy(4000));
        add("gnome_hollow", "crafter", List.of(), hollow(4000, 64));
        add("principle_dropper", "crafter", List.of(), dropper(20, 25, 1000));
    }

    /**
     * The Calciner burns furnace fuel, a burn lasting the fuel's furnace time divided by {@code fuelDivisor}, keeps coal as
     * upkeep and holds {@code ash} Ash in its Ash Pan; its Sulfur and Mercury go straight into its tanks.
     */
    private static MachineStats calciner(int fuelDivisor, int ash) {
        return new MachineStats(1, 0, 0, fuelDivisor, 0, 1, 0, 0, 0, 0, MachineStats.DEFAULT_UPKEEP_ITEM, MachineStats.DEFAULT_UPKEEP_LEVEL, ash);
    }

    /** The Gnome Hollow: each principle tank holds {@code tank} mB, its store {@code store} items of the vein's yield. */
    private static MachineStats hollow(int tank, int store) {
        return new MachineStats(1, 0, 0, 1, 0, 1, 0, 0, tank, 0, "", 0, store);
    }

    /** The Principle Dropper: every {@code interval} ticks it drips up to {@code perDrip} mB of each principle; each tank holds {@code tank} mB. */
    private static MachineStats dropper(int interval, int perDrip, int tank) {
        return new MachineStats(interval, 0, 0, 1, 0, 1, 0, perDrip, tank, 0, "", 0, 0);
    }

    /** An unpowered alchemy machine: recipe time decides the cycle, each principle tank holds {@code tank} mB. */
    private static MachineStats alchemy(int tank) {
        return new MachineStats(1, 0, 0, 1, 0, 1, 0, 0, tank, 0, "", 0, 0);
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        MachineDefaults.register("servus_alchemy:" + path, kind, recipeTypes, stats);
    }
}
