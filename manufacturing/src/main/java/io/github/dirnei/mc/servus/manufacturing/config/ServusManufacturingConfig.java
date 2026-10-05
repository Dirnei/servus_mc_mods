package io.github.dirnei.mc.servus.manufacturing.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Common config of Servus: Manufacturing, {@code config/servus_manufacturing-common.toml}. */
public final class ServusManufacturingConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAX_CHUNK_LOADERS_PER_TEAM = BUILDER
            .comment("How many Chunk Loaders one team may keep active at the same time.")
            .push("chunkLoader")
            .defineInRange("maxChunkLoadersPerTeam", 4, 1, 64);

    private static final ModConfigSpec.BooleanValue CHUNK_LOADER_RESPECTS_FTB_CHUNKS = BUILDER
            .comment("When FTB Chunks is installed, Servus Chunk Loaders stay inactive and point players to FTB Chunks.")
            .define("chunkLoaderRespectsFtbChunks", true);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServusManufacturingConfig() {
    }

    public static int maxChunkLoadersPerTeam() {
        return MAX_CHUNK_LOADERS_PER_TEAM.get();
    }

    public static boolean chunkLoaderRespectsFtbChunks() {
        return CHUNK_LOADER_RESPECTS_FTB_CHUNKS.get();
    }
}
