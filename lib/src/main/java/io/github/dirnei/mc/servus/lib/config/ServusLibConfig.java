package io.github.dirnei.mc.servus.lib.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config of Servus Lib, written to {@code config/servus_lib-common.toml}.
 * Values are read when used, so edits apply without a restart.
 */
public final class ServusLibConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue WRENCH_ROTATES_BLOCKS = BUILDER
            .comment("Whether the wrench rotates blocks that do not handle the wrench themselves.")
            .push("wrench")
            .define("wrenchRotatesBlocks", true);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServusLibConfig() {
    }

    public static boolean wrenchRotatesBlocks() {
        return WRENCH_ROTATES_BLOCKS.get();
    }
}
