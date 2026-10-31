package io.github.dirnei.mc.servus.foundation.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import java.util.List;
import org.junit.jupiter.api.Test;

class MachineProfileTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void parsesFurnaceProfile() {
        MachineProfile profile = MachineProfile.parse(json("""
                {"blocks": ["minecraft:furnace"], "recipe_type": "minecraft:smelting",
                 "sides": {"top": "ingredients", "bottom": "output", "back": "upkeep", "left": "upkeep", "right": "upkeep"},
                 "upkeep": {"item": "minecraft:coal", "level": 16}}"""));
        assertEquals(List.of("minecraft:furnace"), profile.blocks());
        assertEquals("minecraft:smelting", profile.recipeType());
        assertEquals(MachineProfile.Role.INGREDIENTS, profile.role(RelativeSide.TOP));
        assertEquals(MachineProfile.Role.OUTPUT, profile.role(RelativeSide.BOTTOM));
        assertEquals(MachineProfile.Role.UPKEEP, profile.role(RelativeSide.LEFT));
        assertEquals("minecraft:coal", profile.upkeepItem());
        assertEquals(16, profile.upkeepLevel());
    }

    @Test
    void missingSidesAreOff() {
        MachineProfile profile = MachineProfile.parse(json("""
                {"blocks": ["#c:smelters"], "recipe_type": "minecraft:smelting", "sides": {"top": "ingredients"}}"""));
        assertEquals(MachineProfile.Role.OFF, profile.role(RelativeSide.FRONT));
        assertEquals(MachineProfile.Role.OFF, profile.role(RelativeSide.BOTTOM));
    }

    @Test
    void upkeepIsOptional() {
        MachineProfile profile = MachineProfile.parse(json("""
                {"blocks": ["minecraft:smoker"], "recipe_type": "minecraft:smoking"}"""));
        assertEquals("", profile.upkeepItem());
        assertEquals(MachineProfile.DEFAULT_LEVEL, profile.upkeepLevel());
    }

    @Test
    void rejectsMissingRecipeType() {
        assertThrows(IllegalArgumentException.class, () -> MachineProfile.parse(json("{\"blocks\": [\"minecraft:furnace\"]}")));
    }

    @Test
    void upkeepBatchDefaultsToEight() {
        MachineProfile profile = MachineProfile.parse(json("""
                {"blocks": ["minecraft:furnace"], "recipe_type": "minecraft:smelting",
                 "upkeep": {"item": "minecraft:coal", "level": 16}}"""));

        assertEquals(8, profile.upkeepBatch());
    }

    @Test
    void upkeepBatchReadAndClamped() {
        MachineProfile three = MachineProfile.parse(json("""
                {"blocks": ["minecraft:furnace"], "recipe_type": "minecraft:smelting",
                 "upkeep": {"item": "minecraft:coal", "level": 16, "batch": 3}}"""));
        MachineProfile zero = MachineProfile.parse(json("""
                {"blocks": ["minecraft:furnace"], "recipe_type": "minecraft:smelting",
                 "upkeep": {"item": "minecraft:coal", "level": 16, "batch": 0}}"""));

        assertEquals(3, three.upkeepBatch());
        assertEquals(1, zero.upkeepBatch());
    }
}
