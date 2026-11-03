package io.github.dirnei.mc.servus.notes.model;

/** The server's caps on lists, text lengths and goals. */
public record Limits(int maxListsPerPlayer, int maxTasksPerList, int maxNameLength, int maxTaskLength, int maxNotesLength,
                     int maxGoalsPerTask) {

    public static final int DEFAULT_GOALS_PER_TASK = 16;

    public static final Limits DEFAULT = new Limits(50, 200, 40, 120, 500, DEFAULT_GOALS_PER_TASK);

    /** Limits with the default goal cap. */
    public Limits(int maxListsPerPlayer, int maxTasksPerList, int maxNameLength, int maxTaskLength, int maxNotesLength) {
        this(maxListsPerPlayer, maxTasksPerList, maxNameLength, maxTaskLength, maxNotesLength, DEFAULT_GOALS_PER_TASK);
    }
}
