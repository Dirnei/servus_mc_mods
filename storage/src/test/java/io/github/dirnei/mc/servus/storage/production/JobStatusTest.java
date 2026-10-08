package io.github.dirnei.mc.servus.storage.production;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dirnei.mc.servus.storage.production.JobStatus.Status;
import io.github.dirnei.mc.servus.storage.production.JobStatus.Step;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobStatusTest {

    private static Step step(String state, String reason, boolean outOfFuel) {
        return new Step("Crafting Station", state, reason, "crafting", 1, 2, outOfFuel);
    }

    @Test
    void doneAndCancelled() {
        assertEquals(new Status("done", List.of()), JobStatus.of("done", "", "", 0, null));
        assertEquals(new Status("cancelled", List.of()), JobStatus.of("cancelled", "", "", 0, step("producing", "", false)));
    }

    @Test
    void waitingForCpu() {
        assertEquals("waiting_cpu", JobStatus.of("waiting", "", "Oak Planks", 3, step("waiting_station", "", false)).key());
    }

    @Test
    void missingMaterial() {
        assertEquals(new Status("missing", List.of("3", "Oak Planks")),
                JobStatus.of("blocked", "missing", "Oak Planks", 3, step("producing", "", true)));
    }

    @Test
    void outOfFuel() {
        assertEquals(new Status("out_of_fuel", List.of("Crafting Station")), JobStatus.of("running", "", "", 0, step("producing", "busy", true)));
    }

    @Test
    void blockedByStepReason() {
        assertEquals(new Status("blocked", List.of("station busy")), JobStatus.of("running", "", "", 0, step("waiting_station", "station busy", false)));
    }

    @Test
    void waitingForMachineAndIngredients() {
        assertEquals(new Status("waiting_machine", List.of("Crafting Station")), JobStatus.of("running", "", "", 0, step("waiting_station", "", false)));
        assertEquals(new Status("delivering", List.of("Crafting Station")), JobStatus.of("running", "", "", 0, step("delivering", "", false)));
    }

    @Test
    void craftingOneOfTwo() {
        assertEquals(new Status("working", List.of("Crafting Station", "crafting", "1", "2")),
                JobStatus.of("running", "", "", 0, step("producing", "", false)));
    }

    @Test
    void noStepUsesOrderReason() {
        assertEquals(new Status("blocked", List.of("no access point")), JobStatus.of("blocked", "no access point", "", 0, null));
        assertEquals("running", JobStatus.of("running", "", "", 0, null).key());
    }

    @Test
    void craftsDoneCapped() {
        assertEquals(1, JobStatus.craftsDone(4, 4, 2));
        assertEquals(2, JobStatus.craftsDone(12, 4, 2));
        assertEquals(0, JobStatus.craftsDone(3, 0, 2));
    }
}
