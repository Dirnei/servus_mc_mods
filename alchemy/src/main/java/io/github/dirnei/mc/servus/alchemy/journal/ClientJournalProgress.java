package io.github.dirnei.mc.servus.alchemy.journal;

import io.github.dirnei.mc.servus.alchemy.journal.JournalProgress.ChapterProgress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** The client's copy of its team's journal progress, as last sent by the server. */
public final class ClientJournalProgress {

    private static volatile Map<String, ChapterProgress> chapters = Map.of();
    private static volatile Set<String> tried = Set.of();

    private ClientJournalProgress() {
    }

    public static void update(JournalProgressPayload payload) {
        Map<String, ChapterProgress> byId = new LinkedHashMap<>();
        payload.chapters().forEach(chapter -> byId.put(chapter.id(), chapter));
        chapters = Map.copyOf(byId);
        tried = Set.copyOf(payload.tried());
    }

    /** The ids of the calcination recipes the team has tried; empty until something arrived. */
    public static Set<String> tried() {
        return tried;
    }

    /** The chapter's progress; empty while it is locked for the team or nothing arrived yet. */
    public static Optional<ChapterProgress> chapter(String id) {
        return Optional.ofNullable(chapters.get(id));
    }
}
