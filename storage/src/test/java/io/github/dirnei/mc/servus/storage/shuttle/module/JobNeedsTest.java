package io.github.dirnei.mc.servus.storage.shuttle.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JobNeedsTest {

    private static final ShuttleProfile BIG = ShuttleProfile.of(ShuttleTiers.DEFAULT, 3, Optional.empty(), 0, 0);
    private static final ShuttleProfile FIELD = ShuttleProfile.of(ShuttleTiers.DEFAULT, 1, Optional.of(ShuttleProfileTest.HARVESTER), 0, 0);
    private static final ShuttleProfile PROBE = ShuttleProfile.of(ShuttleTiers.DEFAULT, 1, Optional.of(ShuttleProfileTest.PROBE), 0, 0);

    @Test
    void warehouseJobsFitShuttlesWithoutFieldTools() {
        assertTrue(JobNeeds.WAREHOUSE.fits(ShuttleProfile.plain()));
        assertTrue(JobNeeds.WAREHOUSE.fits(BIG));
        assertTrue(JobNeeds.WAREHOUSE.fits(PROBE), "a tool that is not field only still takes warehouse jobs");
    }

    @Test
    void warehouseJobsRefuseFieldOnlyShuttles() {
        assertFalse(JobNeeds.WAREHOUSE.fits(FIELD));
    }

    @Test
    void neededModuleMustBeFitted() {
        JobNeeds harvest = JobNeeds.HARVESTER;
        assertTrue(harvest.fits(FIELD));
        assertFalse(harvest.fits(BIG));
        assertFalse(harvest.fits(ShuttleProfile.plain()));
    }

    @Test
    void fieldJobTakesFieldOnlyShuttle() {
        assertTrue(new JobNeeds(Set.of(), true).fits(FIELD));
    }
}
