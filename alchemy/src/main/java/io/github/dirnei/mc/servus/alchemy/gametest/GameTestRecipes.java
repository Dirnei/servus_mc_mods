package io.github.dirnei.mc.servus.alchemy.gametest;

import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

/**
 * Recipes only the GameTests use, added while recipes load and only in the GameTest server runs, which set the system
 * property {@code servus.gametest.recipes}: a coagulation locked behind Villach, one whose seed another recipe already has, and a
 * quick putrefaction.
 */
public final class GameTestRecipes {

    /** A coagulation of 50 mB Salt into an Echo Shard that needs Villach, seeded by an Echo Shard. */
    public static final Identifier LOCKED_SEED = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gametest/locked_seed");
    /** A coagulation of 50 mB Salt into Flint seeded by Kandis, which the Salt Crystal recipe keeps. */
    public static final Identifier DUPLICATE_SEED = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gametest/duplicate_seed");

    /** Eight putrefiable items into one Humus in 40 ticks in a full bed and 160 in an empty one; it sorts before the shipped recipe. */
    public static final Identifier QUICK_ROT = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "gametest/quick_rot");

    private GameTestRecipes() {
    }

    /** Adds the recipes when this is a GameTest server run. */
    public static void registerWhenTesting() {
        if (Boolean.getBoolean("servus.gametest.recipes")) {
            NeoForge.EVENT_BUS.addListener(ModifyRecipeJsonsEvent.class, event -> {
                event.getRecipeJsons().put(LOCKED_SEED, coagulation("minecraft:echo_shard", "minecraft:echo_shard", "servus_alchemy:villach"));
                event.getRecipeJsons().put(DUPLICATE_SEED, coagulation("minecraft:flint", "servus_alchemy:kandis", null));
                event.getRecipeJsons().put(QUICK_ROT, quickRot());
            });
        }
    }

    private static JsonObject quickRot() {
        JsonObject json = new JsonObject();
        json.addProperty("type", ServusAlchemyIds.MOD_ID + ":putrefaction");
        JsonObject input = new JsonObject();
        input.addProperty("ingredient", "#" + ServusAlchemyIds.MOD_ID + ":putrefiable");
        input.addProperty("count", 8);
        json.add("input", input);
        JsonObject result = new JsonObject();
        result.addProperty("id", ServusAlchemyIds.MOD_ID + ":humus");
        json.add("result", result);
        json.addProperty("time", 40);
        json.addProperty("cold_time", 160);
        return json;
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
