package io.github.dirnei.mc.servus.alchemy.journal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Task counting of Paracelsus' journal. Pure Java: chapters and tasks are plain records, and whether an
 * event matches a task's filter is decided by the caller.
 */
public final class JournalLedger {

    /**
     * One task of a chapter.
     *
     * @param id     unique within its chapter
     * @param event  the event it counts, such as {@code calcine}
     * @param filter item id, {@code #tag} or principle name; empty for any
     * @param count  how much it needs; 1 makes it a first time task
     */
    public record TaskDef(String id, String event, String filter, int count) {

        public TaskDef {
            if (count < 1) {
                throw new IllegalArgumentException("A task needs a count of at least 1: " + id);
            }
        }
    }

    /**
     * One chapter of the journal.
     *
     * @param id     namespaced id
     * @param parent the chapter that must be finished first, if any
     * @param order  sort order in lists
     * @param tasks  what the chapter asks for
     */
    public record ChapterDef(String id, Optional<String> parent, int order, List<TaskDef> tasks) {

        public ChapterDef {
            tasks = List.copyOf(tasks);
        }
    }

    /** One team's progress: counts per {@code chapter/task} and the finished chapters. */
    public static final class TeamJournal {

        private final Map<String, Integer> counts = new HashMap<>();
        private final Set<String> finished = new LinkedHashSet<>();

        public int count(String chapter, String task) {
            return counts.getOrDefault(key(chapter, task), 0);
        }

        public void setCount(String chapter, String task, int count) {
            counts.put(key(chapter, task), count);
        }

        public boolean isFinished(String chapter) {
            return finished.contains(chapter);
        }

        public void markFinished(String chapter) {
            finished.add(chapter);
        }

        public Set<String> finished() {
            return Set.copyOf(finished);
        }

        public Map<String, Integer> counts() {
            return Map.copyOf(counts);
        }

        static String key(String chapter, String task) {
            return chapter + "/" + task;
        }
    }

    private JournalLedger() {
    }

    /** Whether a chapter takes events: not finished, and its parent is finished or it has none. */
    public static boolean isOpen(ChapterDef chapter, TeamJournal journal) {
        return !journal.isFinished(chapter.id())
                && chapter.parent().map(journal::isFinished).orElse(true);
    }

    /**
     * Adds {@code amount} to every matching task of every open chapter, capped at the task's count,
     * and returns the chapters this finished, in the given order.
     */
    public static List<String> record(Collection<ChapterDef> chapters, TeamJournal journal, String event,
                                      Predicate<TaskDef> matches, int amount) {
        List<String> done = new ArrayList<>();
        if (amount <= 0) {
            return done;
        }
        for (ChapterDef chapter : chapters) {
            if (!isOpen(chapter, journal)) {
                continue;
            }
            boolean changed = false;
            for (TaskDef task : chapter.tasks()) {
                if (!task.event().equals(event) || !matches.test(task)) {
                    continue;
                }
                int before = journal.count(chapter.id(), task.id());
                int after = (int) Math.min(task.count(), (long) before + amount);
                if (after != before) {
                    journal.setCount(chapter.id(), task.id(), after);
                    changed = true;
                }
            }
            if (changed && isComplete(chapter, journal)) {
                journal.markFinished(chapter.id());
                done.add(chapter.id());
            }
        }
        return done;
    }

    /** Whether every task of the chapter reached its count. A chapter without tasks is never complete by events. */
    public static boolean isComplete(ChapterDef chapter, TeamJournal journal) {
        if (chapter.tasks().isEmpty()) {
            return false;
        }
        for (TaskDef task : chapter.tasks()) {
            if (journal.count(chapter.id(), task.id()) < task.count()) {
                return false;
            }
        }
        return true;
    }
}
