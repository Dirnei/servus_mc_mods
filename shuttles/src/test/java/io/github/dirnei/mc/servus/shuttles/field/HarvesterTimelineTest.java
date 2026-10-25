package io.github.dirnei.mc.servus.shuttles.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Moment;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Part;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Span;
import io.github.dirnei.mc.servus.shuttles.field.HarvesterTimeline.Stage;
import java.util.List;
import org.junit.jupiter.api.Test;

class HarvesterTimelineTest {

    @Test
    void sharesCoverEachPhaseInOrder() {
        for (Part part : Part.values()) {
            List<Span> spans = HarvesterTimeline.spans(part);
            assertEquals(0, spans.getFirst().from(), 1e-9, part.name());
            assertEquals(1, spans.getLast().to(), 1e-9, part.name());
            for (int i = 0; i < spans.size(); i++) {
                assertTrue(spans.get(i).to() > spans.get(i).from(), part + " " + spans.get(i));
                if (i > 0) {
                    assertEquals(spans.get(i - 1).to(), spans.get(i).from(), 1e-9, part + " " + spans.get(i));
                    assertTrue(spans.get(i).stage().ordinal() > spans.get(i - 1).stage().ordinal(), part + " " + spans.get(i));
                }
            }
        }
        assertEquals(List.of(Stage.DOORS, Stage.WALLS, Stage.RISE), HarvesterTimeline.spans(Part.UNFOLD).stream().map(Span::stage).toList());
        assertEquals(List.of(Stage.REACH, Stage.MOVE_IN, Stage.GRIP, Stage.CUT),
                HarvesterTimeline.spans(Part.SAW).stream().map(Span::stage).toList());
        assertEquals(List.of(Stage.LET_GO, Stage.WAIT, Stage.HOME), HarvesterTimeline.spans(Part.TOPPLE).stream().map(Span::stage).toList());
    }

    @Test
    void foldMirrorsUnfold() {
        for (double p = 0; p <= 1; p += 0.05) {
            Moment fold = HarvesterTimeline.of(Part.FOLD, p);
            Moment unfold = HarvesterTimeline.of(Part.UNFOLD, 1 - p);
            assertEquals(unfold.stage(), fold.stage(), "at " + p);
            assertEquals(unfold.progress(), fold.progress(), 1e-9, "at " + p);
        }
        assertEquals(new Moment(Stage.DOORS, 0), HarvesterTimeline.of(Part.FOLD, 1));
        assertEquals(new Moment(Stage.RISE, 1), HarvesterTimeline.of(Part.READY, 0.3));
    }

    @Test
    void unfoldSharesFromTheDesign() {
        assertEquals(Stage.DOORS, HarvesterTimeline.of(Part.UNFOLD, 0.29).stage());
        assertEquals(Stage.WALLS, HarvesterTimeline.of(Part.UNFOLD, 0.31).stage());
        assertEquals(Stage.RISE, HarvesterTimeline.of(Part.UNFOLD, 0.61).stage());
        assertEquals(0.5, HarvesterTimeline.of(Part.UNFOLD, 0.8).progress(), 1e-9);
    }

    @Test
    void sawEndsWithBarOut() {
        Moment end = HarvesterTimeline.of(Part.SAW, 1);
        assertEquals(Stage.CUT, end.stage());
        assertEquals(1, end.progress(), 1e-9);
        HarvesterPose pose = HarvesterPose.of(new double[] {48, -2.5, 8}, end);
        assertEquals(1, pose.saw(), 1e-9);
        assertEquals(0, pose.a1(), 1e-9);
        assertEquals(Stage.GRIP, HarvesterTimeline.of(Part.SAW, 0.55).stage());
    }

    @Test
    void toppleStartsLettingGo() {
        Moment start = HarvesterTimeline.of(Part.TOPPLE, 0);
        assertEquals(new Moment(Stage.LET_GO, 0), start);
        assertEquals(Stage.WAIT, HarvesterTimeline.of(Part.TOPPLE, 0.5).stage());
        assertEquals(Stage.HOME, HarvesterTimeline.of(Part.TOPPLE, HarvesterTimeline.FALL_PERCENT / 100.0 + 0.01).stage());
        assertEquals(Stage.WAIT, HarvesterTimeline.of(Part.TOPPLE, HarvesterTimeline.FALL_PERCENT / 100.0 - 0.01).stage());
    }

    @Test
    void progressOutsideThePartIsClamped() {
        assertEquals(new Moment(Stage.REACH, 0), HarvesterTimeline.of(Part.SAW, -1));
        assertEquals(new Moment(Stage.HOME, 1), HarvesterTimeline.of(Part.TOPPLE, 3));
    }
}
