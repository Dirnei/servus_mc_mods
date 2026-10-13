package io.github.dirnei.mc.servus.foundation.adapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a Machine Adapter needs to know about a machine: which blocks, which
 * recipe type they run and what each side of the machine is for. Parsed from
 * datapack JSON.
 *
 * @param blocks      block ids, or {@code #tag} ids
 * @param recipeType  the recipe type id, for example {@code minecraft:smelting}
 * @param sides       the role of each side relative to the machine's facing; missing sides are off
 * @param upkeepItem  default upkeep item id, empty for none
 * @param upkeepLevel default upkeep level
 */
public record MachineProfile(List<String> blocks, String recipeType, Map<RelativeSide, Role> sides, String upkeepItem, int upkeepLevel) {

    /** What a side of the machine is used for. */
    public enum Role {
        INGREDIENTS,
        UPKEEP,
        OUTPUT,
        OFF
    }

    public static final int DEFAULT_LEVEL = 16;

    public MachineProfile {
        blocks = List.copyOf(blocks);
        EnumMap<RelativeSide, Role> all = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) {
            all.put(side, sides.getOrDefault(side, Role.OFF));
        }
        sides = Collections.unmodifiableMap(all);
    }

    public Role role(RelativeSide side) {
        return sides.get(side);
    }

    /**
     * Reads a profile: {@code blocks} (array), {@code recipe_type}, {@code sides}
     * (object keyed by front, back, left, right, top, bottom with ingredients,
     * upkeep, output or off) and an optional {@code upkeep} object with item and level.
     *
     * @throws IllegalArgumentException when blocks or recipe_type are missing or a value is unknown
     */
    public static MachineProfile parse(JsonObject json) {
        if (!json.has("blocks") || !json.get("blocks").isJsonArray() || !json.has("recipe_type")) {
            throw new IllegalArgumentException("a machine profile needs blocks and recipe_type");
        }
        List<String> blocks = new ArrayList<>();
        for (JsonElement block : json.getAsJsonArray("blocks")) {
            blocks.add(block.getAsString());
        }
        Map<RelativeSide, Role> sides = new EnumMap<>(RelativeSide.class);
        if (json.has("sides")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("sides").entrySet()) {
                sides.put(RelativeSide.valueOf(entry.getKey().toUpperCase(Locale.ROOT)),
                        Role.valueOf(entry.getValue().getAsString().toUpperCase(Locale.ROOT)));
            }
        }
        String item = "";
        int level = DEFAULT_LEVEL;
        if (json.has("upkeep")) {
            JsonObject upkeep = json.getAsJsonObject("upkeep");
            item = upkeep.has("item") ? upkeep.get("item").getAsString() : "";
            level = upkeep.has("level") ? upkeep.get("level").getAsInt() : DEFAULT_LEVEL;
        }
        return new MachineProfile(blocks, json.get("recipe_type").getAsString(), sides, item, level);
    }
}
