package io.github.dirnei.mc.servus.manufacturing.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ManufacturingMachinesTest {

    @BeforeAll
    static void register() {
        ManufacturingMachines.registerDefaults();
    }

    @Test
    void alchemyMachinesAreRegistered() {
        MachineDefaults.MachineInfo info = MachineDefaults.info("servus_manufacturing:calciner");
        assertEquals("burner", info.kind());
        assertEquals(0, info.stats().energyPerTick());
        assertEquals(2, info.stats().fuelDivisor());
        assertEquals(0, MachineDefaults.stats("servus_manufacturing:coagulator").energyPerTick());
        assertEquals(4000, MachineDefaults.stats("servus_manufacturing:coagulator").fluidCapacity());
    }

    @Test
    void unknownMachineFails() {
        assertThrows(NoSuchElementException.class, () -> MachineDefaults.info("servus_manufacturing:nope"));
    }

    @Test
    void onlyTheCalcinerKeepsUpkeep() {
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            if (info.id().startsWith("servus_manufacturing:")) {
                MachineStats stats = info.stats();
                boolean calciner = info.id().equals("servus_manufacturing:calciner");
                assertEquals(calciner, stats.hasUpkeep(), info.id() + " upkeep");
            }
        }
        assertEquals("minecraft:coal", MachineDefaults.stats("servus_manufacturing:calciner").upkeepItem());
    }
}
