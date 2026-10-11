package io.github.dirnei.mc.servus.storage.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobNeedsTest {

    private static final ShuttleProfile CARGO = ShuttleProfile.of(List.of(ShuttleProfileTest.CARGO_HOLD), 4);
    private static final ShuttleProfile FIELD = ShuttleProfile.of(List.of(ShuttleProfileTest.HARVESTER), 4);
    private static final ShuttleProfile PROBE = ShuttleProfile.of(List.of(ShuttleProfileTest.PROBE), 4);

    @Test
    void warehouseJobsFitPlainAndCargoHold() {
        assertTrue(JobNeeds.WAREHOUSE.fits(ShuttleProfile.PLAIN));
        assertTrue(JobNeeds.WAREHOUSE.fits(CARGO));
        assertTrue(JobNeeds.WAREHOUSE.fits(PROBE), "a tool that is not field only still takes warehouse jobs");
    }

    @Test
    void warehouseJobsRefuseFieldOnlyShuttles() {
        assertFalse(JobNeeds.WAREHOUSE.fits(FIELD));
    }

    @Test
    void neededModuleMustBeFitted() {
        JobNeeds harvest = new JobNeeds(Set.of("harvester"), true);
        assertTrue(harvest.fits(FIELD));
        assertFalse(harvest.fits(CARGO));
        assertFalse(harvest.fits(ShuttleProfile.PLAIN));
    }

    @Test
    void fieldJobTakesFieldOnlyShuttle() {
        assertTrue(new JobNeeds(Set.of(), true).fits(FIELD));
    }
}
