package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * The models of one folder ({@code block} or {@code item}) of a mod, collected
 * by the block state and item model providers and written as JSON.
 *
 * @param <T> the builder type
 */
public class ModelProvider<T extends ModelBuilder<T>> {

    protected final String modid;
    protected final String folder;
    private final Function<Identifier, T> factory;
    private final Map<Identifier, T> generated = new LinkedHashMap<>();

    public ModelProvider(String modid, String folder, Function<Identifier, T> factory) {
        this.modid = modid;
        this.folder = folder;
        this.factory = factory;
    }

    public Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(modid, path);
    }

    public Identifier mcLoc(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    private Identifier location(String path) {
        Identifier location = path.contains(":") ? Identifier.parse(path) : modLoc(path);
        return location.getPath().contains("/") ? location : location.withPrefix(folder + "/");
    }

    /** The builder for the model at {@code path} (in this folder unless the path has one). */
    public T getBuilder(String path) {
        return generated.computeIfAbsent(location(path), factory);
    }

    public T withExistingParent(String name, Identifier parent) {
        return getBuilder(name).parent(getExistingFile(parent));
    }

    public T withExistingParent(String name, String parent) {
        return withExistingParent(name, Identifier.parse(parent.contains(":") ? parent : modid + ":" + parent));
    }

    public ModelFile.ExistingModelFile getExistingFile(Identifier location) {
        return new ModelFile.ExistingModelFile(location);
    }

    public T cubeAll(String name, Identifier texture) {
        return withExistingParent(name, mcLoc("block/cube_all")).texture("all", texture);
    }

    public T cubeBottomTop(String name, Identifier side, Identifier bottom, Identifier top) {
        return withExistingParent(name, mcLoc("block/cube_bottom_top"))
                .texture("side", side).texture("bottom", bottom).texture("top", top);
    }

    public T orientable(String name, Identifier side, Identifier front, Identifier top) {
        return withExistingParent(name, mcLoc("block/orientable"))
                .texture("side", side).texture("front", front).texture("top", top);
    }

    public T cube(String name, Identifier down, Identifier up, Identifier north, Identifier south, Identifier east, Identifier west) {
        return withExistingParent(name, mcLoc("block/cube"))
                .texture("down", down).texture("up", up).texture("north", north)
                .texture("south", south).texture("east", east).texture("west", west)
                .texture("particle", north);
    }

    private final Map<Identifier, String> itemTypes = new HashMap<>();

    /** Names a custom item model type for an item's definition, instead of {@code minecraft:model}. */
    public void itemModelType(Identifier itemId, String type) {
        itemTypes.put(itemId, type);
    }

    /** Writes every model; for item models also the 1.21.4+ item model definition under {@code items/}. */
    CompletableFuture<?> write(CachedOutput cache, PackOutput output) {
        PackOutput.PathProvider models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        PackOutput.PathProvider items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        generated.forEach((location, builder) -> {
            futures.add(DataProvider.saveStable(cache, builder.toJson(), models.json(location)));
            if (folder.equals("item") && location.getPath().startsWith("item/")) {
                Identifier itemId = location.withPath(location.getPath().substring("item/".length()));
                if (!itemId.getPath().contains("/") && BuiltInRegistries.ITEM.containsKey(itemId)) {
                    JsonObject definition = itemDefinition(location);
                    if (itemTypes.containsKey(itemId)) {
                        definition.getAsJsonObject("model").addProperty("type", itemTypes.get(itemId));
                    } else if (Wood.isWoodBlock(itemId)) {
                        // Wood block items retexture their boards per stack.
                        definition.getAsJsonObject("model").addProperty("type", "servus_foundation:wood");
                    }
                    futures.add(DataProvider.saveStable(cache, definition, items.json(itemId)));
                }
            }
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** {@code {"model": {"type": "minecraft:model", "model": "<location>"}}}. */
    static JsonObject itemDefinition(Identifier model) {
        JsonObject inner = new JsonObject();
        inner.addProperty("type", "minecraft:model");
        inner.addProperty("model", model.toString());
        JsonObject root = new JsonObject();
        root.add("model", inner);
        return root;
    }

    /** Item model folder with the {@code basicItem} helper. */
    public static class Items extends ModelProvider<ItemModelBuilder> {

        public Items(String modid) {
            super(modid, "item", ItemModelBuilder::new);
        }

        /** A flat item model with the texture {@code item/<name>}. */
        public ItemModelBuilder basicItem(Item item) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            return getBuilder(id.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", Identifier.fromNamespaceAndPath(id.getNamespace(), "item/" + id.getPath()));
        }

        public ItemModelBuilder basicItem(Identifier itemId) {
            return getBuilder(itemId.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath()));
        }
    }
}
