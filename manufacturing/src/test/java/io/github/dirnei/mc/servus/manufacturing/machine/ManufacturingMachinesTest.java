package io.github.dirnei.mc.servus.manufacturing.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ManufacturingMachinesTest {

    @BeforeAll
    static void register() {
        ManufacturingMachines.registerDefaults();
    }

    private static final Set<String> KINDS = Set.of("burner", "powered", "crafter", "generator");

    @Test
    void everyMachineHasAnEntry() {
        for (String path : new String[] {"burner_extractor", "burner_smelter", "electric_extractor", "crusher",
                "assembler", "research_lab", "chunk_loader"}) {
            MachineDefaults.MachineInfo info = MachineDefaults.info("servus_manufacturing:" + path);
            assertTrue(KINDS.contains(info.kind()), path);
        }
        assertEquals(7, MachineDefaults.all().keySet().stream().filter(id -> id.startsWith("servus_manufacturing:")).count());
    }

    @Test
    void unknownMachineFails() {
        assertThrows(NoSuchElementException.class, () -> MachineDefaults.info("servus_manufacturing:nope"));
    }

    @Test
    void burnerDefaultsHaveNoEnergy() {
        MachineStats stats = MachineDefaults.stats("servus_manufacturing:burner_extractor");

        assertEquals(80, stats.workTicks());
        assertEquals(0, stats.energyPerTick());
        assertEquals(0, stats.energyCapacity());
        assertEquals(1, stats.fuelDivisor());
        assertEquals(1, stats.recipeSlots());
        assertEquals(4, stats.fuelReserve());
    }

    @Test
    void onlyTheBurnerExtractorKeepsAFuelReserve() {
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            int expected = info.id().endsWith("burner_extractor") ? 4 : 0;
            assertEquals(expected, info.stats().fuelReserve(), info.id());
        }
    }

    @Test
    void onlyBurnerMachinesKeepCoalAsUpkeep() {
        Set<String> burners = Set.of("burner_extractor", "burner_smelter");
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            MachineStats stats = info.stats();
            boolean burner = burners.contains(info.id().substring(info.id().indexOf(':') + 1));
            assertEquals(burner, stats.hasUpkeep(), info.id());
            if (burner) {
                assertEquals("minecraft:coal", stats.upkeepItem(), info.id());
                assertEquals(16, stats.upkeepLevel(), info.id());
            }
        }
    }
}
