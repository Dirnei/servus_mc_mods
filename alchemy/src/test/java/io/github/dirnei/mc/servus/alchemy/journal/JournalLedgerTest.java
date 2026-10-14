package io.github.dirnei.mc.servus.alchemy.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JournalLedgerTest {

    private static final ChapterDef VILLACH = new ChapterDef("s:villach", Optional.empty(), 0, List.of(
            new TaskDef("calcine", "calcine", "", 100),
            new TaskDef("salt", "principle", "salt", 1000),
            new TaskDef("crystal", "coagulate", "s:salt_crystal", 1)));
    private static final ChapterDef SCHWAZ = new ChapterDef("s:schwaz", Optional.of("s:villach"), 1, List.of(
            new TaskDef("calcine_more", "calcine", "", 10)));
    private static final List<ChapterDef> BOOK = List.of(VILLACH, SCHWAZ);

    private static boolean filter(TaskDef task, String key) {
        return task.filter().isEmpty() || task.filter().equals(key);
    }

    @Test
    void firstChapterIsOpenAndChildWaits() {
        TeamJournal journal = new TeamJournal();
        assertTrue(JournalLedger.isOpen(VILLACH, journal));
        assertFalse(JournalLedger.isOpen(SCHWAZ, journal));
    }

    @Test
    void eventsCountForMatchingTasksOnly() {
        TeamJournal journal = new TeamJournal();
        JournalLedger.record(BOOK, journal, "calcine", task -> filter(task, "minecraft:cobblestone"), 40);
        JournalLedger.record(BOOK, journal, "principle", task -> filter(task, "mercury"), 500);
        assertEquals(40, journal.count("s:villach", "calcine"));
        assertEquals(0, journal.count("s:villach", "salt"), "Mercury is not Salt");
        assertEquals(0, journal.count("s:schwaz", "calcine_more"), "Schwaz is not open yet");
    }

    @Test
    void countsAreCappedAndTheChapterFinishesOnce() {
        TeamJournal journal = new TeamJournal();
        assertEquals(List.of(), JournalLedger.record(BOOK, journal, "calcine", task -> true, 250));
        assertEquals(100, journal.count("s:villach", "calcine"), "capped at the count");
        JournalLedger.record(BOOK, journal, "principle", task -> filter(task, "salt"), 1000);
        List<String> done = JournalLedger.record(BOOK, journal, "coagulate", task -> filter(task, "s:salt_crystal"), 1);
        assertEquals(List.of("s:villach"), done);
        assertTrue(journal.isFinished("s:villach"));
        assertTrue(JournalLedger.isOpen(SCHWAZ, journal), "the child opens");
        assertEquals(List.of(), JournalLedger.record(BOOK, journal, "coagulate", task -> true, 1), "nothing finishes twice");
    }

    @Test
    void finishedChaptersIgnoreEvents() {
        TeamJournal journal = new TeamJournal();
        journal.markFinished("s:villach");
        JournalLedger.record(BOOK, journal, "calcine", task -> true, 5);
        assertEquals(0, journal.count("s:villach", "calcine"));
        assertEquals(5, journal.count("s:schwaz", "calcine_more"));
    }

    @Test
    void countsSurviveACopy() {
        TeamJournal journal = new TeamJournal();
        JournalLedger.record(BOOK, journal, "calcine", task -> true, 7);
        journal.markFinished("s:other");
        TeamJournal copy = new TeamJournal();
        journal.counts().forEach((key, value) -> {
            String[] parts = key.split("/", 2);
            copy.setCount(parts[0], parts[1], value);
        });
        journal.finished().forEach(copy::markFinished);
        assertEquals(7, copy.count("s:villach", "calcine"));
        assertTrue(copy.isFinished("s:other"));
    }

    @Test
    void zeroCountTasksAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TaskDef("bad", "calcine", "", 0));
    }
}
