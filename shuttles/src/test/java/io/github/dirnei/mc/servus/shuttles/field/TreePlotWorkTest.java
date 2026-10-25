package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TreePlotWorkTest {

    @Test
    void grownTreeIsFelledAndReplanted() {
        assertEquals(new TreePlotWork.Action(true, true), TreePlotWork.plan(PlotState.RIPE, true, 1, 1, true));
    }

    @Test
    void fellOnlyPlantsNothing() {
        assertEquals(new TreePlotWork.Action(true, false), TreePlotWork.plan(PlotState.RIPE, false, 5, 1, true));
    }

    @Test
    void pausedFieldLeavesTreesStanding() {
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.RIPE, true, 1, 1, false));
    }

    @Test
    void emptySpotNeedsASapling() {
        assertEquals(new TreePlotWork.Action(false, true), TreePlotWork.plan(PlotState.PLANTABLE, true, 1, 1, true));
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.PLANTABLE, true, 0, 1, true));
    }

    @Test
    void squareNeedsFour() {
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.PLANTABLE, true, 3, 4, true));
        assertEquals(new TreePlotWork.Action(false, true), TreePlotWork.plan(PlotState.PLANTABLE, true, 4, 4, true));
        assertEquals(new TreePlotWork.Action(true, false), TreePlotWork.plan(PlotState.RIPE, true, 3, 4, true));
    }

    @Test
    void saplingsAndIdlePlotsAreLeftAlone() {
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.GROWING, true, 5, 1, true));
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.IDLE, true, 5, 1, true));
        assertEquals(TreePlotWork.Action.NONE, TreePlotWork.plan(PlotState.BLOCKED, true, 5, 1, true));
    }
}
