package io.github.dirnei.mc.servus.manufacturing.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.List;

/** Manufacturing's machines in the shared {@link MachineDefaults} table. */
public final class ManufacturingMachines {

    /** Pseudo recipe type of extractors: a deposit's yield, no inputs. */
    public static final String EXTRACTION = "servus:extraction";
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
        add("burner_extractor", "burner", List.of(EXTRACTION), MachineStats.burner(80, 4));
        add("burner_smelter", "burner", List.of("minecraft:smelting"), MachineStats.burner(80));
        add("electric_extractor", "powered", List.of(EXTRACTION), MachineStats.powered(40, 20, 8000));
        add("crusher", "powered", List.of("servus_manufacturing:crushing"), MachineStats.powered(60, 20, 8000));
        add("assembler", "crafter", List.of("servus_manufacturing:assembling", "minecraft:crafting"),
                MachineStats.crafter(20, 20, 8000, 2));
        add("coal_generator", "generator", List.of(), MachineStats.generator(40, 16000, 4));
        add("research_lab", "powered", List.of(RESEARCH), MachineStats.powered(1, 10, 4000));
        add("chunk_loader", "powered", List.of(), MachineStats.powered(1, 10, 4000));
        add("burner_pump", "burner", List.of(), MachineStats.burnerPump(20, 250, 4000, 2));
        add("electric_pump", "powered", List.of(), MachineStats.poweredPump(20, 20, 8000, 500, 4000, 2));
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        MachineDefaults.register("servus_manufacturing:" + path, kind, recipeTypes, stats);
    }
}
