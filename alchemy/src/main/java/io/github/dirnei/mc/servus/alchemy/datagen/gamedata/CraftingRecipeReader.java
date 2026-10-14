package io.github.dirnei.mc.servus.alchemy.datagen.gamedata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataJson.Crafting;
import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataJson.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads a crafting recipe file into a game data entry. The shape decides, not the type: {@code pattern} plus
 * {@code key} is a grid (vanilla shaped and Servus types built on it), {@code ingredients} plus {@code result} is
 * shapeless, anything else is special. Plain Java over Gson, no Minecraft classes.
 */
public final class CraftingRecipeReader {

    private CraftingRecipeReader() {
    }

    public static Crafting read(String id, JsonObject json) {
        String type = json.get("type").getAsString();
        if (json.has("pattern") && json.has("key")) {
            List<String> pattern = new ArrayList<>();
            json.getAsJsonArray("pattern").forEach(row -> pattern.add(row.getAsString()));
            Map<String, Stack> key = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("key").entrySet()) {
                Stack stack = ingredient(entry.getValue(), 1);
                if (stack != null) {
                    key.put(entry.getKey(), stack);
                }
            }
            List<Stack> uses = new ArrayList<>();
            for (String row : pattern) {
                for (char symbol : row.toCharArray()) {
                    Stack stack = key.get(String.valueOf(symbol));
                    if (stack != null) {
                        uses.add(stack);
                    }
                }
            }
            return new Crafting(id, type, false, pattern, key, summed(uses), outputs(json));
        }
        if (json.has("ingredients") && json.has("result")) {
            List<Stack> uses = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("ingredients")) {
                Stack stack = ingredient(element, 1);
                if (stack != null) {
                    uses.add(stack);
                }
            }
            return new Crafting(id, type, false, null, null, summed(uses), outputs(json));
        }
        return new Crafting(id, type, true, null, null, List.of(), List.of());
    }

    /**
     * An item or tag ingredient: {@code "minecraft:stick"} or {@code "#minecraft:planks"}, a sized
     * {@code {"ingredient": ..., "count": n}}, or an {@code {"item": ...}} or {@code {"tag": ...}} object. A list keeps its first entry; other ingredient types give null.
     */
    static Stack ingredient(JsonElement element, int count) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return array.isEmpty() ? null : ingredient(array.get(0), count);
        }
        if (element.isJsonPrimitive()) {
            String id = element.getAsString();
            return id.startsWith("#") ? Stack.tag(id.substring(1), count) : Stack.item(id, count);
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("ingredient")) {
            return ingredient(object.get("ingredient"), count);
        }
        if (object.has("tag")) {
            return Stack.tag(object.get("tag").getAsString(), count);
        }
        if (object.has("item")) {
            return Stack.item(object.get("item").getAsString(), count);
        }
        return null;
    }

    private static List<Stack> outputs(JsonObject json) {
        if (!json.has("result")) {
            return List.of();
        }
        if (json.get("result").isJsonPrimitive()) {
            return List.of(Stack.item(json.get("result").getAsString(), 1));
        }
        JsonObject result = json.getAsJsonObject("result");
        int count = result.has("count") ? result.get("count").getAsInt() : 1;
        return List.of(Stack.item(result.get("id").getAsString(), count));
    }

    /** One stack per item or tag with the counts added up, sorted by item id or {@code #tag}. */
    private static List<Stack> summed(List<Stack> uses) {
        Map<String, Stack> sums = new TreeMap<>();
        for (Stack use : uses) {
            String key = use.item() != null ? use.item() : "#" + use.tag();
            sums.merge(key, use, (a, b) -> new Stack(a.item(), a.tag(), a.count() + b.count()));
        }
        return new ArrayList<>(sums.values());
    }
}
