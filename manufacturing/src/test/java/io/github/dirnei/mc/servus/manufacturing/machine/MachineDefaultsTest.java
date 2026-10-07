package io.github.dirnei.mc.servus.manufacturing.machine;

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
                "assembler", "coal_generator", "research_lab", "chunk_loader"}) {
            MachineDefaults.MachineInfo info = MachineDefaults.info("servus_manufacturing:" + path);
            assertTrue(KINDS.contains(info.kind()), path);
        }
        assertEquals(8, MachineDefaults.all().size());
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
    }

    @Test
    void generatorBurnsFourTimesFaster() {
        MachineStats stats = MachineDefaults.stats("servus_manufacturing:coal_generator");

        assertEquals(40, stats.energyPerTick());
        assertEquals(4, stats.fuelDivisor());
        assertEquals("generator", MachineDefaults.info("servus_manufacturing:coal_generator").kind());
    }
}
