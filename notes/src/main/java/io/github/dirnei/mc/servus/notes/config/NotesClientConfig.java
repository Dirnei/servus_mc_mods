package io.github.dirnei.mc.servus.notes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config of Servus: Notes, written to {@code config/servus_notes-client.toml}.
 * Values are read when used, so edits apply without a restart.
 */
public final class NotesClientConfig {

    /** Where the HUD pin sits on the screen. */
    public enum HudCorner {
        TOP_LEFT,
        TOP_RIGHT,
        LEFT_MIDDLE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder().push("hud");

    private static final ModConfigSpec.EnumValue<HudCorner> HUD_CORNER = BUILDER
            .comment("Where the pinned list is drawn: TOP_LEFT, TOP_RIGHT or LEFT_MIDDLE.")
            .defineEnum("hudCorner", HudCorner.TOP_LEFT);

    private static final ModConfigSpec.IntValue HUD_LINES = BUILDER
            .comment("How many tasks and goals the pinned list shows below its name before \"+n more\".")
            .defineInRange("hudLines", 4, 2, 8);

    private static final ModConfigSpec.BooleanValue TICK_TOASTS = BUILDER
            .pop()
            .push("toasts")
            .comment("Whether a toast tells you when another player ticks a task on a Team list or a list you own or are a guest of.")
            .define("tickToasts", true);

    public static final ModConfigSpec SPEC = BUILDER.pop().build();

    private NotesClientConfig() {
    }

    public static HudCorner hudCorner() {
        return SPEC.isLoaded() ? HUD_CORNER.get() : HudCorner.TOP_LEFT;
    }

    public static int hudLines() {
        return SPEC.isLoaded() ? HUD_LINES.get() : 4;
    }

    public static boolean tickToasts() {
        return !SPEC.isLoaded() || TICK_TOASTS.get();
    }
}
