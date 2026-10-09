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
}
