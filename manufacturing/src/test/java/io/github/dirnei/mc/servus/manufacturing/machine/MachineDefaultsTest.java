package io.github.dirnei.mc.servus.manufacturing.machine;

import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MachineDefaultsTest {

    private static final Set<String> KINDS = Set.of("burner", "powered", "crafter", "generator");

    @Test
    void everyMachineHasAnEntry() {
        for (String path : new String[] {"burner_extractor", "burner_smelter", "electric_extractor", "crusher",
                "assembler", "coal_generator", "research_lab", "chunk_loader", "burner_pump", "electric_pump"}) {
            MachineDefaults.MachineInfo info = MachineDefaults.info("servus_manufacturing:" + path);
            assertTrue(KINDS.contains(info.kind()), path);
        }
        assertEquals(10, MachineDefaults.all().size());
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
    void generatorBurnsFourTimesFaster() {
        MachineStats stats = MachineDefaults.stats("servus_manufacturing:coal_generator");

        assertEquals(40, stats.energyPerTick());
        assertEquals(4, stats.fuelDivisor());
        assertEquals("generator", MachineDefaults.info("servus_manufacturing:coal_generator").kind());
    }

    @Test
    void pumpsHaveFluidStatsAndOthersNone() {
        MachineStats burner = MachineDefaults.stats("servus_manufacturing:burner_pump");
        MachineStats electric = MachineDefaults.stats("servus_manufacturing:electric_pump");

        assertEquals(250, burner.fluidPerCycle());
        assertEquals(4000, burner.fluidCapacity());
        assertEquals(2, burner.minSourceNeighbours());
        assertEquals(0, burner.energyPerTick());
        assertTrue(electric.fluidPerCycle() * burner.workTicks() > burner.fluidPerCycle() * electric.workTicks(),
                "the Electric Pump pumps faster");
        assertEquals(20, electric.energyPerTick());
        assertEquals(0, MachineDefaults.stats("servus_manufacturing:crusher").fluidCapacity());
    }

    @Test
    void onlyBurnerMachinesKeepCoalAsUpkeep() {
        Set<String> burners = Set.of("burner_extractor", "burner_smelter", "burner_pump", "coal_generator");
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
