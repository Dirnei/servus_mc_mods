package io.github.dirnei.mc.servus.brewery.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config of Servus: Brewery, written to {@code config/servus_brewery-client.toml}.
 * Values are read when used, so edits apply without a restart.
 */
public final class ServusBreweryClientConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue SCREEN_WOBBLE = BUILDER
            .comment("Whether being Drunk or Plastered makes the screen wobble.",
                    "Off keeps every other Rausch effect; Nausea from other sources still wobbles.")
            .push("rausch")
            .define("screenWobble", true);

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServusBreweryClientConfig() {
    }

    public static boolean screenWobble() {
        return SCREEN_WOBBLE.get();
    }
}
