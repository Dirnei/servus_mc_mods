package io.github.dirnei.mc.servus.foundation.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config of Servus: Foundation, written to {@code config/servus_foundation-common.toml}.
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

    private static final ModConfigSpec.IntValue MAX_LOADED_CHUNKS_PER_TEAM = BUILDER
            .pop()
            .comment("How many chunks one team may keep loaded at the same time with Ender Anchors and woken warehouse sites.",
                    "A chunk held by several of them counts once.")
            .push("enderAnchor")
            .defineInRange("maxLoadedChunksPerTeam", 49, 1, 1024);

    private static final ModConfigSpec.BooleanValue ENDER_ANCHOR_RESPECTS_FTB_CHUNKS = BUILDER
            .comment("Set to true to leave chunk loading to FTB Chunks: while FTB Chunks is installed, Servus Ender Anchors",
                    "then stay inactive and point players to FTB Chunks, and warehouse sites do not wake.",
                    "Off by default: anchors and site wakes work next to FTB Chunks.")
            .define("enderAnchorRespectsFtbChunks", false);

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

    public static int maxLoadedChunksPerTeam() {
        return MAX_LOADED_CHUNKS_PER_TEAM.get();
    }

    public static boolean enderAnchorRespectsFtbChunks() {
        return ENDER_ANCHOR_RESPECTS_FTB_CHUNKS.get();
    }

    /** Sets {@link #enderAnchorRespectsFtbChunks()} in memory without writing the file; for GameTests. */
    public static void setEnderAnchorRespectsFtbChunks(boolean respects) {
        ENDER_ANCHOR_RESPECTS_FTB_CHUNKS.set(respects);
    }
}
