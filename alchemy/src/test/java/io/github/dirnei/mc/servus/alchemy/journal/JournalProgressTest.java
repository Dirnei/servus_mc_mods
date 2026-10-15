package io.github.dirnei.mc.servus.alchemy.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.ChapterProgress;
import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.TaskProgress;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JournalProgressTest {

    private static final ChapterDef VILLACH = new ChapterDef("s:villach", Optional.empty(), 0, List.of(
            new TaskDef("calcine", "calcine", "", 100),
            new TaskDef("crystal", "coagulate", "", 1)));
    private static final ChapterDef SCHWAZ = new ChapterDef("s:schwaz", Optional.of("s:villach"), 1, List.of(
            new TaskDef("gnome", "bind", "", 1)));
    private static final List<ChapterDef> BOOK = List.of(VILLACH, SCHWAZ);

    @Test
    void openChapterShowsItsCountsAndLockedChaptersAreLeftOut() {
        TeamJournal journal = new TeamJournal();
        journal.setCount("s:villach", "calcine", 40);
        List<ChapterProgress> shown = JournalProgress.snapshot(BOOK, journal);
        assertEquals(1, shown.size(), "Schwaz is still locked");
        ChapterProgress villach = shown.getFirst();
        assertFalse(villach.finished());
        assertEquals(List.of(new TaskProgress("calcine", 40, 100), new TaskProgress("crystal", 0, 1)), villach.tasks());
        assertFalse(villach.tasks().getFirst().done());
    }

    @Test
    void finishedChapterShowsEveryTaskDoneAndOpensTheNext() {
        TeamJournal journal = new TeamJournal();
        journal.markFinished("s:villach");
        List<ChapterProgress> shown = JournalProgress.snapshot(BOOK, journal);
        assertEquals(List.of("s:villach", "s:schwaz"), shown.stream().map(ChapterProgress::id).toList());
        assertTrue(shown.getFirst().finished());
        assertTrue(shown.getFirst().tasks().stream().allMatch(TaskProgress::done), "a finished chapter shows its tasks done");
        assertFalse(shown.get(1).finished());
    }
}
