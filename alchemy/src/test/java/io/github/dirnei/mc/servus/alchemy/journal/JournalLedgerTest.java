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
    void chainOpensOneAfterAnother() {
        ChapterDef wandering = new ChapterDef("s:wandering_years", Optional.of("s:schwaz"), 2, List.of(
                new TaskDef("citrinitas_craft", "athanor", "tier:citrinitas", 1),
                new TaskDef("gnomes", "bind", "", 4)));
        ChapterDef basel = new ChapterDef("s:basel_salzburg", Optional.of("s:wandering_years"), 3, List.of(
                new TaskDef("crafts", "athanor", "", 16)));
        List<ChapterDef> book = List.of(VILLACH, SCHWAZ, wandering, basel);
        TeamJournal journal = new TeamJournal();
        journal.markFinished("s:villach");
        JournalLedger.record(book, journal, "athanor", task -> filter(task, "tier:citrinitas"), 1);
        assertEquals(0, journal.count("s:wandering_years", "citrinitas_craft"), "Wandering years waits for Schwaz");
        assertEquals(0, journal.count("s:basel_salzburg", "crafts"), "Basel waits too");
        journal.markFinished("s:schwaz");
        JournalLedger.record(book, journal, "athanor", task -> filter(task, "tier:citrinitas"), 1);
        assertEquals(1, journal.count("s:wandering_years", "citrinitas_craft"));
        assertEquals(0, journal.count("s:basel_salzburg", "crafts"), "Basel opens only after Wandering years");
        assertEquals(List.of("s:wandering_years"), JournalLedger.record(book, journal, "bind", task -> true, 4));
        JournalLedger.record(book, journal, "athanor", task -> filter(task, "tier:rubedo"), 1);
        assertEquals(1, journal.count("s:basel_salzburg", "crafts"));
    }

    @Test
    void finishingAChapterFinishesItsParentsFirst() {
        TeamJournal journal = new TeamJournal();
        assertEquals(List.of("s:villach", "s:schwaz"), JournalLedger.finish(BOOK, journal, "s:schwaz"));
        assertTrue(journal.isFinished("s:villach"));
        assertTrue(journal.isFinished("s:schwaz"));
        assertEquals(1000, journal.count("s:villach", "salt"));
        assertEquals(10, journal.count("s:schwaz", "calcine_more"));
    }

    @Test
    void finishingStopsAtFinishedChapters() {
        TeamJournal journal = new TeamJournal();
        journal.markFinished("s:villach");
        assertEquals(List.of("s:schwaz"), JournalLedger.finish(BOOK, journal, "s:schwaz"));
        assertEquals(0, journal.count("s:villach", "calcine"), "a finished parent is left as it was");
        assertEquals(List.of(), JournalLedger.finish(BOOK, journal, "s:schwaz"), "already finished");
    }

    @Test
    void finishingAnUnknownChapterChangesNothing() {
        TeamJournal journal = new TeamJournal();
        assertEquals(List.of(), JournalLedger.finish(BOOK, journal, "s:nowhere"));
        ChapterDef orphan = new ChapterDef("s:orphan", Optional.of("s:missing"), 5, List.of());
        assertEquals(List.of(), JournalLedger.finish(List.of(VILLACH, orphan), journal, "s:orphan"));
        assertTrue(journal.finished().isEmpty());
    }

    @Test
    void zeroCountTasksAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TaskDef("bad", "calcine", "", 0));
    }
}
