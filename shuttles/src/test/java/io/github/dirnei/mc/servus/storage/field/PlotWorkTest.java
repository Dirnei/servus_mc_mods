package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PlotWorkTest {

    @Test
    void ripeIsHarvestedAndReplantedInPlantMode() {
        assertEquals(new PlotWork.Action(true, false, true), PlotWork.plan(PlotState.RIPE, true, false, true));
        assertEquals(new PlotWork.Action(true, false, false), PlotWork.plan(PlotState.RIPE, false, true, true));
    }

    @Test
    void tillableIsTilledOnlyInPlantMode() {
        assertEquals(new PlotWork.Action(false, true, true), PlotWork.plan(PlotState.TILLABLE, true, true, true));
        assertEquals(new PlotWork.Action(false, true, false), PlotWork.plan(PlotState.TILLABLE, true, false, true));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.TILLABLE, false, true, true));
    }

    @Test
    void plantableNeedsASeed() {
        assertEquals(new PlotWork.Action(false, false, true), PlotWork.plan(PlotState.PLANTABLE, true, true, true));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.PLANTABLE, true, false, true));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.PLANTABLE, false, true, true));
    }

    @Test
    void growingAndBlockedAreLeftAlone() {
        assertFalse(PlotWork.plan(PlotState.GROWING, true, true, true).any());
        assertFalse(PlotWork.plan(PlotState.BLOCKED, true, true, true).any());
    }

    @Test
    void pausedHarvestLeavesRipePlots() {
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.RIPE, true, true, false));
        assertEquals(new PlotWork.Action(false, false, true), PlotWork.plan(PlotState.PLANTABLE, true, true, false));
        assertEquals(new PlotWork.Action(false, true, true), PlotWork.plan(PlotState.TILLABLE, true, true, false));
    }
}
