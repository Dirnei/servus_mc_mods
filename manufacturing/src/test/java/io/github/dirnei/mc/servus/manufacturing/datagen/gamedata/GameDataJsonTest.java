package io.github.dirnei.mc.servus.manufacturing.datagen.gamedata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Crafting;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Deposit;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.GameData;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Machine;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Recipe;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Stack;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Tech;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameDataJsonTest {

    private static GameData sample(boolean reversed) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("work_ticks", 80);
        stats.put("energy_per_tick", 0);
        List<Machine> machines = List.of(
                new Machine("servus_manufacturing:burner_smelter", "burner", List.of("minecraft:smelting"), stats),
                new Machine("servus_manufacturing:burner_extractor", "burner", List.of("servus:extraction"), stats));
        List<Recipe> recipes = List.of(
                new Recipe("servus_manufacturing:assembling/red_pack", "servus_manufacturing:assembling",
                        List.of(Stack.item("servus_manufacturing:iron_gear", 1), Stack.tag("c:wires", 2)),
                        List.of(Stack.item("servus_manufacturing:red_pack", 1)), 100),
                new Recipe("servus_manufacturing:assembling/iron_gear", "servus_manufacturing:assembling",
                        List.of(Stack.item("minecraft:iron_ingot", 2)),
                        List.of(Stack.item("servus_manufacturing:iron_gear", 1)), 20));
        List<Crafting> crafting = List.of(
                new Crafting("servus_storage:zone_label", "minecraft:crafting_shapeless", false, null, null,
                        List.of(Stack.tag("c:dyes", 1), Stack.item("minecraft:paper", 1)),
                        List.of(Stack.item("servus_storage:zone_label", 1))),
                new Crafting("servus_foundation:power_cable", "minecraft:crafting_shaped", false,
                        List.of("CCC", "RGR", "CCC"),
                        Map.of("R", Stack.item("minecraft:redstone", 1), "C", Stack.item("minecraft:copper_ingot", 1)),
                        List.of(Stack.item("minecraft:copper_ingot", 6)),
                        List.of(Stack.item("servus_foundation:power_cable", 8))),
                new Crafting("servus_foundation:facade", "servus_foundation:facade", true, null, null, List.of(), List.of()));
        Map<String, String> names = new LinkedHashMap<>();
        names.put("minecraft:iron_ingot", "Iron Ingot");
        names.put("minecraft:coal", "Coal \"black\"");
        return new GameData("0.1.0",
                reversed ? machines.reversed() : machines,
                reversed ? recipes.reversed() : recipes,
                reversed ? crafting.reversed() : crafting,
                List.of(new Deposit("servus_manufacturing:iron", "servus_manufacturing:iron_deposit",
                        Stack.item("minecraft:raw_iron", 1))),
                List.of(new Tech("servus_manufacturing:crusher", List.of("servus_manufacturing:electric_extractor"),
                        List.of(Stack.item("servus_manufacturing:red_pack", 15)), 100, List.of())),
                Map.of("minecraft:coal", 1600),
                names);
    }

    @Test
    void sameDataGivesSameTextInAnyInputOrder() {
        assertEquals(GameDataJson.write(sample(false)), GameDataJson.write(sample(true)));
    }

    @Test
    void entriesAreSortedById() {
        String json = GameDataJson.write(sample(false));

        assertTrue(json.indexOf("burner_extractor") < json.indexOf("burner_smelter"));
        assertTrue(json.indexOf("assembling/iron_gear") < json.indexOf("assembling/red_pack"));
        assertTrue(json.indexOf("\"minecraft:coal\": {") < json.indexOf("\"minecraft:iron_ingot\": {"));
    }

    @Test
    void topLevelKeysInFixedOrder() {
        String json = GameDataJson.write(sample(false));
        String[] keys = {"\"schema\": 2", "\"mod_version\"", "\"machines\"", "\"recipes\"", "\"crafting\"", "\"deposits\"",
                "\"techs\"", "\"fuels\"", "\"items\""};
        for (int i = 1; i < keys.length; i++) {
            assertTrue(json.indexOf(keys[i - 1]) < json.indexOf(keys[i]), keys[i]);
        }
    }

    @Test
    void machineStatsKeepTheirOrderAndStacksTheirKind() {
        String json = GameDataJson.write(sample(false));

        assertTrue(json.indexOf("\"work_ticks\": 80") < json.indexOf("\"energy_per_tick\": 0"));
        assertTrue(json.contains("{\"tag\": \"c:wires\", \"count\": 2}"));
        assertTrue(json.contains("{\"item\": \"minecraft:raw_iron\", \"count\": 1}"));
        assertTrue(json.contains("\"Coal \\\"black\\\"\""));
    }

    @Test
    void craftingWritesPatternAndSpecialOnlyWhenSet() {
        String json = GameDataJson.write(sample(false));
        String facade = json.substring(json.indexOf("\"servus_foundation:facade\": {"),
                json.indexOf("\"servus_foundation:power_cable\": {"));
        String cable = json.substring(json.indexOf("\"servus_foundation:power_cable\": {"),
                json.indexOf("\"servus_storage:zone_label\": {"));
        String label = json.substring(json.indexOf("\"servus_storage:zone_label\": {"), json.indexOf("\"deposits\""));

        assertTrue(facade.contains("\"special\": true"));
        assertFalse(facade.contains("pattern"));
        assertFalse(cable.contains("special"));
        assertTrue(cable.indexOf("\"pattern\"") < cable.indexOf("\"key\""));
        assertTrue(cable.indexOf("\"key\"") < cable.indexOf("\"inputs\""));
        assertTrue(cable.indexOf("\"C\": {\"item\": \"minecraft:copper_ingot\"}") < cable.indexOf("\"R\": {"));
        assertFalse(label.contains("pattern") || label.contains("special"));
        assertTrue(label.contains("{\"tag\": \"c:dyes\", \"count\": 1}"));
    }

    @Test
    void noTimestampOrCommit() {
        String json = GameDataJson.write(sample(false));

        assertFalse(json.contains("commit"));
        assertFalse(json.contains("time\": \"") || json.contains("date"));
        assertTrue(json.endsWith("}\n"));
    }
}
