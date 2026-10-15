package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.ChapterDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TaskDef;
import io.github.dirnei.mc.servus.alchemy.journal.JournalLedger.TeamJournal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** What a team's journal shows: open and finished chapters with each task's count. Pure Java. */
public final class JournalProgress {

    /**
     * One task as the book shows it.
     *
     * @param id     unique within its chapter
     * @param count  how far the team is
     * @param needed the task's count
     */
    public record TaskProgress(String id, int count, int needed) {

        public boolean done() {
            return count >= needed;
        }
    }

    /**
     * One open or finished chapter.
     *
     * @param id       namespaced chapter id
     * @param finished whether the team finished it
     * @param tasks    its tasks in data order
     */
    public record ChapterProgress(String id, boolean finished, List<TaskProgress> tasks) {

        public ChapterProgress {
            tasks = List.copyOf(tasks);
        }
    }

    private JournalProgress() {
    }

    /** The chapters a team has open or finished, in the given order; locked chapters are left out. */
    public static List<ChapterProgress> snapshot(Collection<ChapterDef> chapters, TeamJournal journal) {
        List<ChapterProgress> shown = new ArrayList<>();
        for (ChapterDef chapter : chapters) {
            boolean finished = journal.isFinished(chapter.id());
            if (!finished && !JournalLedger.isOpen(chapter, journal)) {
                continue;
            }
            List<TaskProgress> tasks = new ArrayList<>();
            for (TaskDef task : chapter.tasks()) {
                int count = finished ? task.count() : Math.min(task.count(), journal.count(chapter.id(), task.id()));
                tasks.add(new TaskProgress(task.id(), count, task.count()));
            }
            shown.add(new ChapterProgress(chapter.id(), finished, tasks));
        }
        return shown;
    }
}
