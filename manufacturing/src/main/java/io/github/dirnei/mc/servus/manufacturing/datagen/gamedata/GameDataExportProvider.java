package io.github.dirnei.mc.servus.manufacturing.datagen.gamedata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Deposit;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.GameData;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Machine;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Recipe;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Stack;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Tech;
import io.github.dirnei.mc.servus.manufacturing.deposit.DepositDefinition;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineDefaults;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.recipe.AssemblingRecipeSerializer;
import io.github.dirnei.mc.servus.manufacturing.recipe.CrushingRecipeSerializer;
import io.github.dirnei.mc.servus.manufacturing.research.TechDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Exports the effective game data (machine stats merged with code defaults, recipes, deposits, techs, fuels and
 * item names) to the documentation site. Runs last in the data run, so this run's recipes and lang file exist.
 *
 * <p>Paths come from system properties set by the Manufacturing data run: {@code servus.gamedata.output} (the
 * JSON file) and {@code servus.gamedata.resources} (the hand written {@code src/main/resources}). Without them the
 * provider does nothing, so other data runs are unaffected.
 */
public final class GameDataExportProvider implements DataProvider {

    private static final String NS = ServusManufacturingIds.MOD_ID;
    private static final String SMELTING = "minecraft:smelting";
    private static final String BLASTING = "minecraft:blasting";
    private static final int VANILLA_COOKING_TIME = 200;
    private static final List<String> ALWAYS_FUELS = List.of("minecraft:coal", "minecraft:charcoal");

    private final PackOutput output;

    public GameDataExportProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        String target = System.getProperty("servus.gamedata.output");
        String resources = System.getProperty("servus.gamedata.resources");
        if (target == null || resources == null) {
            ServusManufacturing.LOGGER.info("Game data export skipped: servus.gamedata.output not set");
            return CompletableFuture.completedFuture(null);
        }
        try {
            String json = GameDataJson.write(collect(Path.of(resources)));
            Path file = Path.of(target);
            Files.createDirectories(file.getParent());
            if (!Files.exists(file) || !Files.readString(file, StandardCharsets.UTF_8).equals(json)) {
                Files.writeString(file, json, StandardCharsets.UTF_8);
            }
            ServusManufacturing.LOGGER.info("Game data exported to {}", file);
            return CompletableFuture.completedFuture(null);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private GameData collect(Path resources) throws IOException {
        Path mainData = resources.resolve("data").resolve(NS);
        Path generatedData = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(NS);

        List<Machine> machines = machines(mainData.resolve("machine"));
        List<Recipe> recipes = new ArrayList<>(servusRecipes(generatedData.resolve("recipe")));
        List<Deposit> deposits = deposits(mainData.resolve("deposit"));
        List<Tech> techs = techs(mainData.resolve(NS).resolve("tech"));

        Set<String> referenced = new HashSet<>();
        recipes.forEach(recipe -> referenceAll(referenced, recipe));
        deposits.forEach(deposit -> reference(referenced, deposit.yield()));
        techs.forEach(tech -> tech.cost().forEach(stack -> reference(referenced, stack)));
        recipes.addAll(vanillaCooking(referenced));
        recipes.forEach(recipe -> referenceAll(referenced, recipe));

        Map<String, Integer> fuels = fuels(referenced);
        referenced.addAll(fuels.keySet());
        return new GameData(modVersion(), machines, recipes, deposits, techs, fuels, names(referenced));
    }

    // Machines

    private static List<Machine> machines(Path dir) throws IOException {
        List<Machine> machines = new ArrayList<>();
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            Path file = dir.resolve(ResourceLocation.parse(info.id()).getPath() + ".json");
            MachineStats stats = info.stats();
            if (Files.exists(file)) {
                stats = decode(MachineStats.Partial.CODEC, readJson(file), file).over(stats);
            }
            Map<String, Integer> values = new LinkedHashMap<>();
            values.put("work_ticks", stats.workTicks());
            values.put("energy_per_tick", stats.energyPerTick());
            values.put("energy_capacity", stats.energyCapacity());
            values.put("fuel_divisor", stats.fuelDivisor());
            values.put("items_per_tick", stats.itemsPerTick());
            values.put("recipe_slots", stats.recipeSlots());
            machines.add(new Machine(info.id(), info.kind(), info.recipeTypes(), values));
        }
        return machines;
    }

    // Recipes

    private static List<Recipe> servusRecipes(Path dir) throws IOException {
        List<Recipe> recipes = new ArrayList<>();
        for (Path file : jsonFiles(dir)) {
            String id = NS + ":" + relativeId(dir, file);
            JsonObject json = readJson(file).getAsJsonObject();
            String type = json.get("type").getAsString();
            switch (type) {
                case NS + ":assembling" -> recipes.add(new Recipe(id, type,
                        sizedIngredients(json.getAsJsonArray("ingredients")),
                        List.of(result(json.get("result"))),
                        intOr(json, "time", AssemblingRecipeSerializer.DEFAULT_TIME)));
                case NS + ":crushing" -> recipes.add(new Recipe(id, type,
                        List.of(sizedIngredient(json.get("input"))),
                        List.of(result(json.get("result"))),
                        intOr(json, "time", CrushingRecipeSerializer.DEFAULT_TIME)));
                case SMELTING, BLASTING -> recipes.add(cooking(id, type, json));
                default -> {
                    // Crafting recipes are build costs, not part of production chains.
                }
            }
        }
        return recipes;
    }

    /** Vanilla smelting and blasting recipes whose input is referenced, closed over their results. */
    private static List<Recipe> vanillaCooking(Set<String> referenced) {
        Map<String, Recipe> all = new TreeMap<>();
        try (VanillaPackResources vanilla = ServerPacksSource.createVanillaPackSource()) {
            vanilla.listResources(PackType.SERVER_DATA, "minecraft", "recipe", (location, supplier) -> {
                try (InputStream in = supplier.get(); Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String type = json.get("type").getAsString();
                    if (type.equals(SMELTING) || type.equals(BLASTING)) {
                        String path = location.getPath();
                        String id = "minecraft:" + path.substring("recipe/".length(), path.length() - ".json".length());
                        all.put(id, cooking(id, type, json));
                    }
                } catch (IOException | RuntimeException e) {
                    ServusManufacturing.LOGGER.warn("Skipping vanilla recipe {}: {}", location, e.getMessage());
                }
            });
        }
        Set<String> known = new HashSet<>(referenced);
        Map<String, Recipe> picked = new TreeMap<>();
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Recipe recipe : all.values()) {
                if (!picked.containsKey(recipe.id())
                        && recipe.inputs().stream().anyMatch(stack -> stack.item() != null && known.contains(stack.item()))) {
                    picked.put(recipe.id(), recipe);
                    recipe.outputs().forEach(stack -> known.add(stack.item()));
                    grew = true;
                }
            }
        }
        return new ArrayList<>(picked.values());
    }

    private static Recipe cooking(String id, String type, JsonObject json) {
        return new Recipe(id, type, List.of(ingredient(json.get("ingredient"), 1)), List.of(result(json.get("result"))),
                intOr(json, "cookingtime", VANILLA_COOKING_TIME));
    }

    private static List<Stack> sizedIngredients(JsonArray array) {
        List<Stack> stacks = new ArrayList<>();
        array.forEach(element -> stacks.add(sizedIngredient(element)));
        return stacks;
    }

    private static Stack sizedIngredient(JsonElement element) {
        return ingredient(element, intOr(element.getAsJsonObject(), "count", 1));
    }

    /** An ingredient as item or tag. A list ingredient keeps its first entry. */
    private static Stack ingredient(JsonElement element, int count) {
        JsonObject object = element.isJsonArray() ? element.getAsJsonArray().get(0).getAsJsonObject() : element.getAsJsonObject();
        if (object.has("tag")) {
            return Stack.tag(object.get("tag").getAsString(), count);
        }
        return Stack.item(object.get("item").getAsString(), count);
    }

    private static Stack result(JsonElement element) {
        JsonObject object = element.getAsJsonObject();
        return Stack.item(object.get("id").getAsString(), intOr(object, "count", 1));
    }

    // Deposits and techs

    private static List<Deposit> deposits(Path dir) throws IOException {
        List<Deposit> deposits = new ArrayList<>();
        for (Path file : jsonFiles(dir)) {
            DepositDefinition definition = decode(DepositDefinition.CODEC, readJson(file), file);
            deposits.add(new Deposit(NS + ":" + relativeId(dir, file),
                    BuiltInRegistries.BLOCK.getKey(definition.block()).toString(), stack(definition.yield())));
        }
        return deposits;
    }

    private static List<Tech> techs(Path dir) throws IOException {
        List<Tech> techs = new ArrayList<>();
        for (Path file : jsonFiles(dir)) {
            TechDefinition definition = decode(TechDefinition.CODEC, readJson(file), file);
            techs.add(new Tech(NS + ":" + relativeId(dir, file),
                    definition.parents().stream().map(ResourceLocation::toString).toList(),
                    definition.cost().stream().map(GameDataExportProvider::stack).toList(),
                    definition.ticksPerPack(),
                    definition.unlocks().stream().map(ResourceLocation::toString).toList()));
        }
        return techs;
    }

    // Fuels and names

    /** Burn times from NeoForge's furnace fuel data map, for referenced items and coal and charcoal. */
    private static Map<String, Integer> fuels(Set<String> referenced) throws IOException {
        Map<String, Integer> fuels = new TreeMap<>();
        JsonObject map = classpathJson("data/neoforge/data_maps/item/furnace_fuels.json")
                .orElseThrow(() -> new IOException("NeoForge furnace fuel data map not found"));
        for (Map.Entry<String, JsonElement> entry : map.getAsJsonObject("values").entrySet()) {
            String item = entry.getKey();
            if (!item.startsWith("#") && (referenced.contains(item) || ALWAYS_FUELS.contains(item))) {
                fuels.put(item, entry.getValue().getAsJsonObject().get("burn_time").getAsInt());
            }
        }
        return fuels;
    }

    private Map<String, String> names(Set<String> items) throws IOException {
        Map<String, String> lang = new LinkedHashMap<>();
        classpathJson("assets/minecraft/lang/en_us.json").ifPresent(json -> json.entrySet()
                .forEach(entry -> lang.put(entry.getKey(), entry.getValue().getAsString())));
        Path ownLang = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(NS).resolve("lang/en_us.json");
        if (Files.exists(ownLang)) {
            readJson(ownLang).getAsJsonObject().entrySet()
                    .forEach(entry -> lang.put(entry.getKey(), entry.getValue().getAsString()));
        }
        Map<String, String> names = new TreeMap<>();
        for (String id : items) {
            if (id == null) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            String name = lang.get(item.getDescriptionId());
            names.put(id, name != null ? name : titleCase(ResourceLocation.parse(id).getPath()));
        }
        return names;
    }

    static String titleCase(String path) {
        StringBuilder name = new StringBuilder();
        for (String word : path.split("_")) {
            if (!word.isEmpty()) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return name.toString();
    }

    // Helpers

    private static void referenceAll(Set<String> referenced, Recipe recipe) {
        recipe.inputs().forEach(stack -> reference(referenced, stack));
        recipe.outputs().forEach(stack -> reference(referenced, stack));
    }

    private static void reference(Set<String> referenced, Stack stack) {
        if (stack.item() != null) {
            referenced.add(stack.item());
        }
    }

    private static Stack stack(ItemStack stack) {
        return Stack.item(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount());
    }

    private static String modVersion() {
        return ModList.get().getModContainerById(NS)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    private static <T> T decode(Codec<T> codec, JsonElement json, Path file) throws IOException {
        DataResult<T> result = codec.parse(JsonOps.INSTANCE, json);
        return result.result().orElseThrow(() -> new IOException("Invalid " + file + ": "
                + result.error().map(DataResult.Error::message).orElse("")));
    }

    private static List<Path> jsonFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(file -> file.toString().endsWith(".json")).sorted().toList();
        }
    }

    private static String relativeId(Path dir, Path file) {
        String relative = dir.relativize(file).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".json".length());
    }

    private static int intOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static JsonElement readJson(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        }
    }

    private static Optional<JsonObject> classpathJson(String path) throws IOException {
        try (InputStream in = GameDataExportProvider.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                ServusManufacturing.LOGGER.warn("Game data export: {} not on the classpath", path);
                return Optional.empty();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
            }
        }
    }

    @Override
    public String getName() {
        return "Servus game data export";
    }
}
