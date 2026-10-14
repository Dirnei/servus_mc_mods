package io.github.dirnei.mc.servus.manufacturing.datagen.gamedata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturing;
import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Crafting;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Deposit;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.GameData;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Machine;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Recipe;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Stack;
import io.github.dirnei.mc.servus.manufacturing.datagen.gamedata.GameDataJson.Tech;
import java.io.File;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.item.Item;
import net.neoforged.fml.ModList;

/**
 * Exports the effective game data (machine stats merged with code defaults, recipes, deposits, techs, fuels and
 * item names) to the documentation site. Runs last in the data run, so this run's recipes and lang file exist.
 *
 * <p>Paths come from system properties set by the Manufacturing data run: {@code servus.gamedata.output} (the
 * JSON file), {@code servus.gamedata.resources} (the hand written {@code src/main/resources} of every mod) and
 * {@code servus.gamedata.generated} (the generated resource folders of all Servus mods, joined with the path
 * separator). Recipes and language files of the other mods are read from those folders as plain files, so mods
 * that are not loaded in this run still take part. Without the properties the provider does nothing, so other
 * data runs are unaffected.
 */
public final class GameDataExportProvider implements DataProvider {

    private static final String NS = ServusManufacturingIds.MOD_ID;
    private static final String SMELTING = "minecraft:smelting";
    private static final String BLASTING = "minecraft:blasting";
    private static final int VANILLA_COOKING_TIME = 200;
    private static final List<String> ALWAYS_FUELS = List.of("minecraft:coal", "minecraft:charcoal");

    public GameDataExportProvider() {
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        String target = System.getProperty("servus.gamedata.output");
        String resources = System.getProperty("servus.gamedata.resources");
        String generated = System.getProperty("servus.gamedata.generated");
        if (target == null || resources == null || generated == null) {
            ServusManufacturing.LOGGER.info("Game data export skipped: servus.gamedata.output not set");
            return CompletableFuture.completedFuture(null);
        }
        try {
            List<Path> generatedRoots = Stream.of(generated.split(File.pathSeparator)).map(Path::of).toList();
            List<Path> resourceRoots = Stream.of(resources.split(File.pathSeparator)).map(Path::of).toList();
            String json = GameDataJson.write(collect(resourceRoots, generatedRoots));
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

    private GameData collect(List<Path> resourceRoots, List<Path> generatedRoots) throws IOException {
        List<Machine> machines = machines(resourceRoots);
        List<Recipe> recipes = new ArrayList<>();
        List<Crafting> crafting = new ArrayList<>();
        for (Path root : generatedRoots) {
            for (Path namespace : servusNamespaces(root.resolve("data"))) {
                servusRecipes(namespace.getFileName().toString(), namespace.resolve("recipe"), recipes, crafting);
            }
        }
        // Deposits and techs stay empty for the docs schema.
        List<Deposit> deposits = List.of();
        List<Tech> techs = List.of();

        Set<String> referenced = new HashSet<>();
        recipes.forEach(recipe -> referenceAll(referenced, recipe));
        deposits.forEach(deposit -> reference(referenced, deposit.yield()));
        techs.forEach(tech -> tech.cost().forEach(stack -> reference(referenced, stack)));
        recipes.addAll(vanillaCooking(referenced));
        recipes.forEach(recipe -> referenceAll(referenced, recipe));

        Map<String, Integer> fuels = fuels(referenced);
        referenced.addAll(fuels.keySet());
        machines.forEach(machine -> referenced.add(machine.id()));
        // Crafting entries only add names: they never feed the vanilla cooking closure or the fuels.
        crafting.forEach(entry -> {
            entry.inputs().forEach(stack -> reference(referenced, stack));
            entry.outputs().forEach(stack -> reference(referenced, stack));
        });
        return new GameData(modVersion(), machines, recipes, crafting, deposits, techs, fuels,
                names(referenced, generatedRoots));
    }

    /** Each machine's stats file sits at {@code data/<namespace>/machine/<path>.json} in its own mod's resources. */
    private static List<Machine> machines(List<Path> resourceRoots) throws IOException {
        List<Machine> machines = new ArrayList<>();
        for (MachineDefaults.MachineInfo info : MachineDefaults.all().values()) {
            Identifier id = Identifier.parse(info.id());
            MachineStats stats = info.stats();
            for (Path root : resourceRoots) {
                Path file = root.resolve("data").resolve(id.getNamespace()).resolve("machine").resolve(id.getPath() + ".json");
                if (Files.exists(file)) {
                    stats = decode(MachineStats.Partial.CODEC, readJson(file), file).over(stats);
                }
            }
            Map<String, Integer> values = new LinkedHashMap<>();
            values.put("work_ticks", stats.workTicks());
            values.put("energy_per_tick", stats.energyPerTick());
            values.put("energy_capacity", stats.energyCapacity());
            values.put("fuel_divisor", stats.fuelDivisor());
            values.put("items_per_tick", stats.itemsPerTick());
            values.put("recipe_slots", stats.recipeSlots());
            values.put("fuel_reserve", stats.fuelReserve());
            values.put("fluid_per_cycle", stats.fluidPerCycle());
            values.put("fluid_capacity", stats.fluidCapacity());
            values.put("min_source_neighbours", stats.minSourceNeighbours());
            values.put("item_capacity", stats.itemCapacity());
            values.put("upkeep_level", stats.hasUpkeep() ? stats.upkeepLevel() : 0);
            machines.add(new Machine(info.id(), info.kind(), info.recipeTypes(), values));
        }
        return machines;
    }

    /** The {@code servus_*} namespace folders under a generated {@code data} or {@code assets} folder, sorted. */
    private static List<Path> servusNamespaces(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(dir)) {
            return dirs.filter(Files::isDirectory)
                    .filter(namespace -> namespace.getFileName().toString().startsWith("servus_"))
                    .sorted()
                    .toList();
        }
    }

    /** Production recipe types go to {@code recipes}, everything else to {@code crafting}. */
    private static void servusRecipes(String namespace, Path dir, List<Recipe> recipes, List<Crafting> crafting)
            throws IOException {
        for (Path file : jsonFiles(dir)) {
            String id = namespace + ":" + relativeId(dir, file);
            JsonObject json = readJson(file).getAsJsonObject();
            String type = json.get("type").getAsString();
            switch (type) {
                case NS + ":calcination", NS + ":coagulation" -> {
                }
                case SMELTING, BLASTING -> recipes.add(cooking(id, type, json));
                default -> crafting.add(CraftingRecipeReader.read(id, json));
            }
        }
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

    /** An ingredient as item or tag. A list ingredient keeps its first entry. */
    private static Stack ingredient(JsonElement element, int count) {
        Stack stack = CraftingRecipeReader.ingredient(element, count);
        if (stack == null) {
            throw new IllegalArgumentException("Unsupported ingredient " + element);
        }
        return stack;
    }

    private static Stack result(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return Stack.item(element.getAsString(), 1);
        }
        JsonObject object = element.getAsJsonObject();
        return Stack.item(object.get("id").getAsString(), intOr(object, "count", 1));
    }

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

    /**
     * Display names: vanilla ids by their registry description id, Servus ids by language key ({@code item.} then
     * {@code block.}), because mods that are not loaded in this run are not in the registry.
     */
    private static Map<String, String> names(Set<String> items, List<Path> generatedRoots) throws IOException {
        Map<String, String> lang = new LinkedHashMap<>();
        classpathJson("assets/minecraft/lang/en_us.json").ifPresent(json -> json.entrySet()
                .forEach(entry -> lang.put(entry.getKey(), entry.getValue().getAsString())));
        for (Path root : generatedRoots) {
            for (Path namespace : servusNamespaces(root.resolve("assets"))) {
                Path file = namespace.resolve("lang/en_us.json");
                if (Files.exists(file)) {
                    readJson(file).getAsJsonObject().entrySet()
                            .forEach(entry -> lang.put(entry.getKey(), entry.getValue().getAsString()));
                }
            }
        }
        Map<String, String> names = new TreeMap<>();
        for (String id : items) {
            if (id == null) {
                continue;
            }
            Identifier location = Identifier.parse(id);
            String name;
            if (location.getNamespace().startsWith("servus_")) {
                String suffix = location.getNamespace() + "." + location.getPath().replace('/', '.');
                name = lang.getOrDefault("item." + suffix, lang.get("block." + suffix));
            } else {
                Item item = BuiltInRegistries.ITEM.getValue(location);
                name = lang.get(item.getDescriptionId());
            }
            names.put(id, name != null ? name : titleCase(location.getPath()));
        }
        return names;
    }

    /** An id path as English words: {@code caput_mortuum} gives Caput Mortuum. */
    public static String titleCase(String path) {
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

    private static void referenceAll(Set<String> referenced, Recipe recipe) {
        recipe.inputs().forEach(stack -> reference(referenced, stack));
        recipe.outputs().forEach(stack -> reference(referenced, stack));
    }

    private static void reference(Set<String> referenced, Stack stack) {
        if (stack.item() != null) {
            referenced.add(stack.item());
        }
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
