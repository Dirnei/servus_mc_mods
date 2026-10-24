package io.github.dirnei.mc.servus.alchemy.gametest;

import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

/**
 * Recipes only the GameTests use, added while recipes load and only in the GameTest server runs, which set the system
 * property {@code servus.gametest.recipes}: a coagulation locked behind Villach and one whose seed another recipe already has.
 */
public final class GameTestRecipes {

    /** A coagulation of 50 mB Salt into an Echo Shard that needs Villach, seeded by an Echo Shard. */
    public static final Identifier LOCKED_SEED = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gametest/locked_seed");
    /** A coagulation of 50 mB Salt into Flint seeded by Kandis, which the Salt Crystal recipe keeps. */
    public static final Identifier DUPLICATE_SEED = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gametest/duplicate_seed");

    private GameTestRecipes() {
    }

    /** Adds the recipes when this is a GameTest server run. */
    public static void registerWhenTesting() {
        if (Boolean.getBoolean("servus.gametest.recipes")) {
            NeoForge.EVENT_BUS.addListener(ModifyRecipeJsonsEvent.class, event -> {
                event.getRecipeJsons().put(LOCKED_SEED, coagulation("minecraft:echo_shard", "minecraft:echo_shard", "servus_alchemy:villach"));
                event.getRecipeJsons().put(DUPLICATE_SEED, coagulation("minecraft:flint", "servus_alchemy:kandis", null));
            });
        }
    }

    private static JsonObject coagulation(String result, String seed, String chapter) {
        JsonObject json = new JsonObject();
        json.addProperty("type", ServusAlchemyIds.MOD_ID + ":coagulation");
        JsonObject input = new JsonObject();
        input.addProperty("salt", 50);
        json.add("input", input);
        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("id", result);
        json.add("result", resultJson);
        json.addProperty("seed", seed);
        if (chapter != null) {
            json.addProperty("chapter", chapter);
        }
        return json;
    }
}
