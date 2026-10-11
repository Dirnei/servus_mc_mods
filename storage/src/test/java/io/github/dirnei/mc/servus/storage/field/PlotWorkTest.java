package io.github.dirnei.mc.servus.storage.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PlotWorkTest {

    @Test
    void ripeIsHarvestedAndReplantedInPlantMode() {
        assertEquals(new PlotWork.Action(true, false, true), PlotWork.plan(PlotState.RIPE, true, false));
        assertEquals(new PlotWork.Action(true, false, false), PlotWork.plan(PlotState.RIPE, false, true));
    }

    @Test
    void tillableIsTilledOnlyInPlantMode() {
        assertEquals(new PlotWork.Action(false, true, true), PlotWork.plan(PlotState.TILLABLE, true, true));
        assertEquals(new PlotWork.Action(false, true, false), PlotWork.plan(PlotState.TILLABLE, true, false));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.TILLABLE, false, true));
    }

    @Test
    void plantableNeedsASeed() {
        assertEquals(new PlotWork.Action(false, false, true), PlotWork.plan(PlotState.PLANTABLE, true, true));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.PLANTABLE, true, false));
        assertEquals(PlotWork.Action.NONE, PlotWork.plan(PlotState.PLANTABLE, false, true));
    }

    @Test
    void growingAndBlockedAreLeftAlone() {
        assertFalse(PlotWork.plan(PlotState.GROWING, true, true).any());
        assertFalse(PlotWork.plan(PlotState.BLOCKED, true, true).any());
    }
}
