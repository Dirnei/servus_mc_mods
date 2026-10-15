package io.github.dirnei.mc.servus.alchemy.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AlchemyMachineDefaultsTest {

    @BeforeAll
    static void register() {
        AlchemyMachineDefaults.registerDefaults();
    }

    @Test
    void alchemyMachinesAreRegistered() {
        MachineDefaults.MachineInfo info = MachineDefaults.info("servus_alchemy:calciner");
        assertEquals("burner", info.kind());
        assertEquals(0, info.stats().energyPerTick());
        assertEquals(2, info.stats().fuelDivisor());
        assertEquals(0, MachineDefaults.stats("servus_alchemy:coagulator").energyPerTick());
        assertEquals(4000, MachineDefaults.stats("servus_alchemy:coagulator").fluidCapacity());
    }

    @Test
    void dropperDripsByItsStats() {
        MachineStats stats = MachineDefaults.stats("servus_alchemy:principle_dropper");
        assertEquals(20, stats.workTicks());
        assertEquals(25, stats.fluidPerCycle());
        assertEquals(1000, stats.fluidCapacity());
    }

    @Test
    void unknownMachineFails() {
        assertThrows(NoSuchElementException.class, () -> MachineDefaults.info("servus_alchemy:nope"));
    }

    @Test
    void onlyTheCalcinerKeepsUpkeep() {
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            if (info.id().startsWith("servus_alchemy:")) {
                MachineStats stats = info.stats();
                boolean calciner = info.id().equals("servus_alchemy:calciner");
                assertEquals(calciner, stats.hasUpkeep(), info.id() + " upkeep");
            }
        }
        assertEquals("minecraft:coal", MachineDefaults.stats("servus_alchemy:calciner").upkeepItem());
    }
}
