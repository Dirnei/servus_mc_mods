package io.github.dirnei.mc.servus.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config of Servus Lib, written to {@code config/servus_lib-common.toml}.
 * Values are read when used, so edits apply without a restart.
 */
public final class ServusFoundationConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue WRENCH_ROTATES_BLOCKS = BUILDER
            .comment("Whether the Configuration Carrot rotates blocks that do not handle the wrench role themselves.")
            .push("wrench")
            .define("wrenchRotatesBlocks", true);

    private static final ModConfigSpec.IntValue PLACEMENT_ASSIST_REACH = BUILDER
            .pop()
            .comment("How many blocks a sneak click may walk along a run of rails or ducts to place at its end.")
            .push("placement")
            .defineInRange("placementAssistReach", 16, 1, 64);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServusFoundationConfig() {
    }

    public static boolean wrenchRotatesBlocks() {
        return WRENCH_ROTATES_BLOCKS.get();
    }

    public static int placementAssistReach() {
        return PLACEMENT_ASSIST_REACH.get();
    }
}
