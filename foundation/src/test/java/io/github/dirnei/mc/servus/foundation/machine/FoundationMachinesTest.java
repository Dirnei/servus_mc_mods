package io.github.dirnei.mc.servus.foundation.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FoundationMachinesTest {

    @BeforeAll
    static void register() {
        FoundationMachines.registerDefaults();
    }

    @Test
    void registeringTwiceDoesNothing() {
        FoundationMachines.registerDefaults();
        assertTrue(MachineDefaults.all().containsKey("servus_foundation:coal_generator"));
    }

    @Test
    void generatorBurnsFourTimesFaster() {
        MachineStats stats = MachineDefaults.stats("servus_foundation:coal_generator");

        assertEquals(40, stats.energyPerTick());
        assertEquals(4, stats.fuelDivisor());
        assertEquals("generator", MachineDefaults.info("servus_foundation:coal_generator").kind());
        assertTrue(stats.hasUpkeep());
        assertEquals("minecraft:coal", stats.upkeepItem());
    }

    @Test
    void pumpsHaveFluidStats() {
        MachineStats burner = MachineDefaults.stats("servus_foundation:burner_pump");
        MachineStats electric = MachineDefaults.stats("servus_foundation:electric_pump");

        assertEquals(250, burner.fluidPerCycle());
        assertEquals(4000, burner.fluidCapacity());
        assertEquals(2, burner.minSourceNeighbours());
        assertEquals(0, burner.energyPerTick());
        assertTrue(burner.hasUpkeep(), "the Burner Pump keeps coal stocked");
        assertTrue(electric.fluidPerCycle() * burner.workTicks() > burner.fluidPerCycle() * electric.workTicks(),
                "the Electric Pump pumps faster");
        assertEquals(20, electric.energyPerTick());
        assertTrue(!electric.hasUpkeep(), "the Electric Pump needs no upkeep");
        assertEquals(0, MachineDefaults.stats("servus_foundation:coal_generator").fluidCapacity());
    }
}
