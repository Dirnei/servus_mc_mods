package io.github.dirnei.mc.servus.manufacturing.machine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Code defaults of every machine, the values a machine uses for fields its datapack file leaves out.
 * One table so the machines and the game data export read the same numbers.
 */
public final class MachineDefaults {

    /** Pseudo recipe type of extractors: a deposit's yield, no inputs. */
    public static final String EXTRACTION = "servus:extraction";
    /** Pseudo recipe type of the research lab: one pack per {@code ticks_per_pack}. */
    public static final String RESEARCH = "servus:research";

    /**
     * @param id          machine id, also the name of its stats file
     * @param kind        burner, powered, crafter or generator
     * @param recipeTypes recipe types the machine runs, empty for machines without recipes
     * @param stats       the defaults
     */
    public record MachineInfo(String id, String kind, List<String> recipeTypes, MachineStats stats) {
    }

    private static final Map<String, MachineInfo> MACHINES = new LinkedHashMap<>();

    static {
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

    private MachineDefaults() {
    }

    private static void add(String path, String kind, List<String> recipeTypes, MachineStats stats) {
        String id = "servus_manufacturing:" + path;
        MACHINES.put(id, new MachineInfo(id, kind, recipeTypes, stats));
    }

    /** Every machine, in a fixed order. */
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
