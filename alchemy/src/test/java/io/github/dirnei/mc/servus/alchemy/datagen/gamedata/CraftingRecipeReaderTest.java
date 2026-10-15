package io.github.dirnei.mc.servus.alchemy.datagen.gamedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataJson.Crafting;
import io.github.dirnei.mc.servus.alchemy.datagen.gamedata.GameDataJson.Stack;
import java.util.List;
import org.junit.jupiter.api.Test;

class CraftingRecipeReaderTest {

    private static Crafting read(String id, String json) {
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        return CraftingRecipeReader.read(id, object);
    }

    @Test
    void shapedRecipeSumsItsGrid() {
        Crafting cable = read("servus_foundation:power_cable", """
                {"type": "minecraft:crafting_shaped", "category": "redstone",
                 "key": {"C": {"item": "minecraft:copper_ingot"}, "G": {"item": "minecraft:glass"},
                         "R": {"item": "minecraft:redstone"}},
                 "pattern": ["CCC", "RGR", "CCC"],
                 "result": {"count": 8, "id": "servus_foundation:power_cable"}}
                """);

        assertFalse(cable.special());
        assertEquals(List.of("CCC", "RGR", "CCC"), cable.pattern());
        assertEquals(List.of(Stack.item("minecraft:copper_ingot", 6), Stack.item("minecraft:glass", 1),
                Stack.item("minecraft:redstone", 2)), cable.inputs());
        assertEquals(List.of(Stack.item("servus_foundation:power_cable", 8)), cable.outputs());
    }

    @Test
    void servusGridTypeKeepsRowsAndTags() {
        Crafting rack = read("servus_shuttles:heavy_rack", """
                {"type": "servus_shuttles:heavy_rack", "category": "misc",
                 "key": {"I": {"item": "minecraft:iron_ingot"}, "P": {"tag": "minecraft:planks"}},
                 "pattern": ["I I", "PPP", "I I"],
                 "result": {"count": 1, "id": "servus_shuttles:heavy_rack"}}
                """);

        assertEquals("servus_shuttles:heavy_rack", rack.type());
        assertEquals(List.of("I I", "PPP", "I I"), rack.pattern());
        assertEquals(Stack.tag("minecraft:planks", 1), rack.key().get("P"));
        assertTrue(rack.inputs().contains(Stack.item("minecraft:iron_ingot", 4)));
        assertTrue(rack.inputs().contains(Stack.tag("minecraft:planks", 3)));
    }

    @Test
    void shapelessRecipeHasNoPattern() {
        Crafting label = read("servus_shuttles:zone_label", """
                {"type": "minecraft:crafting_shapeless", "category": "equipment",
                 "ingredients": [{"item": "minecraft:paper"}, {"tag": "c:dyes"}],
                 "result": {"count": 1, "id": "servus_shuttles:zone_label"}}
                """);

        assertNull(label.pattern());
        assertNull(label.key());
        assertEquals(List.of(Stack.tag("c:dyes", 1), Stack.item("minecraft:paper", 1)), label.inputs());
    }

    @Test
    void recipeWithoutIngredientsIsSpecial() {
        Crafting facade = read("servus_foundation:facade", """
                {"type": "servus_foundation:facade", "category": "misc"}
                """);

        assertTrue(facade.special());
        assertTrue(facade.inputs().isEmpty());
        assertTrue(facade.outputs().isEmpty());
    }

    @Test
    void listIngredientKeepsItsFirstEntryAndCountDefaultsToOne() {
        Crafting crafting = read("servus_foundation:sample", """
                {"type": "minecraft:crafting_shapeless",
                 "ingredients": [[{"item": "minecraft:coal"}, {"item": "minecraft:charcoal"}], {"item": "minecraft:stick"}],
                 "result": {"id": "minecraft:torch"}}
                """);

        assertEquals(List.of(Stack.item("minecraft:coal", 1), Stack.item("minecraft:stick", 1)), crafting.inputs());
        assertEquals(List.of(Stack.item("minecraft:torch", 1)), crafting.outputs());
    }
}
