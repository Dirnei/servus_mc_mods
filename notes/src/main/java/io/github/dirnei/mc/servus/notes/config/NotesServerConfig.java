package io.github.dirnei.mc.servus.notes.config;

import io.github.dirnei.mc.servus.notes.model.Limits;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config of Servus: Notes, written to {@code serverconfig/servus_notes-server.toml} of each world.
 * Values are read when used, so edits apply without a restart.
 */
public final class NotesServerConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder().push("limits");

    private static final ModConfigSpec.IntValue MAX_LISTS_PER_PLAYER = BUILDER
            .comment("How many lists one player may own.")
            .defineInRange("maxListsPerPlayer", Limits.DEFAULT.maxListsPerPlayer(), 1, 1000);

    private static final ModConfigSpec.IntValue MAX_TASKS_PER_LIST = BUILDER
            .comment("How many tasks, subtasks included, one list may hold.")
            .defineInRange("maxTasksPerList", Limits.DEFAULT.maxTasksPerList(), 1, 2000);

    private static final ModConfigSpec.IntValue MAX_NAME_LENGTH = BUILDER
            .comment("The longest list name, in characters.")
            .defineInRange("maxNameLength", Limits.DEFAULT.maxNameLength(), 1, 200);

    private static final ModConfigSpec.IntValue MAX_TASK_LENGTH = BUILDER
            .comment("The longest task text, in characters.")
            .defineInRange("maxTaskLength", Limits.DEFAULT.maxTaskLength(), 1, 1000);

    private static final ModConfigSpec.IntValue MAX_NOTES_LENGTH = BUILDER
            .comment("The longest notes of one task, in characters.")
            .defineInRange("maxNotesLength", Limits.DEFAULT.maxNotesLength(), 0, 5000);

    private static final ModConfigSpec.IntValue MAX_GOALS_PER_TASK = BUILDER
            .comment("How many item goals one task may hold.")
            .defineInRange("maxGoalsPerTask", Limits.DEFAULT.maxGoalsPerTask(), 0, 200);

    public static final ModConfigSpec SPEC = BUILDER.pop().build();

    private NotesServerConfig() {
    }

    /** The configured limits, or the defaults while no server config is loaded. */
    public static Limits limits() {
        if (!SPEC.isLoaded()) {
            return Limits.DEFAULT;
        }
        return new Limits(MAX_LISTS_PER_PLAYER.get(), MAX_TASKS_PER_LIST.get(), MAX_NAME_LENGTH.get(),
                MAX_TASK_LENGTH.get(), MAX_NOTES_LENGTH.get(), MAX_GOALS_PER_TASK.get());
    }
}
