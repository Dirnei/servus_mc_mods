package io.github.dirnei.mc.servus.alchemy.hollow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.dirnei.mc.servus.alchemy.HexColor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class VeinFilesTest {

    static List<String> veins() {
        return List.of("iron", "copper", "gold", "redstone", "diamond");
    }

    private static JsonObject load(String vein) throws IOException {
        String path = "/data/servus_alchemy/" + VeinData.DIRECTORY + "/" + vein + ".json";
        try (InputStream in = VeinFilesTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "the vein " + vein + " should ship");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @ParameterizedTest
    @MethodSource("veins")
    void shippedVeinNamesYieldGrowthAndColour(String vein) throws IOException {
        JsonObject json = load(vein);
        assertTrue(json.getAsJsonObject("yield").has("id"), vein + " names its yield");
        for (String field : List.of("steps", "step_ticks", "yield_ticks")) {
            assertTrue(json.get(field).getAsInt() >= 1, vein + " has " + field + " of at least 1");
        }
        assertTrue(HexColor.parse(json.get("color").getAsString()).isPresent(), vein + " has a #rrggbb colour");
    }

    @ParameterizedTest
    @MethodSource("veins")
    void onlySeedsPlantSoNoSamplesOrChapter(String vein) throws IOException {
        JsonObject json = load(vein);
        assertFalse(json.has("samples"), vein + " lists no samples");
        assertFalse(json.has("chapter"), vein + " names no chapter");
    }
}
