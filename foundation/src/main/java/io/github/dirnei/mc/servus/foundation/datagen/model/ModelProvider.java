package io.github.dirnei.mc.servus.foundation.datagen.model;

import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.wood.Wood;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

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
    private final Map<Identifier, String> itemTypes = new HashMap<>();
    private final Map<Identifier, JsonObject> rawItemModels = new LinkedHashMap<>();
    private final Set<Identifier> oversizedInGui = new HashSet<>();

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

    /** Writes an item's definition with this item model object as is (for loader models without a model file). */
    public void itemModelDefinition(Identifier itemId, JsonObject model) {
        rawItemModels.put(itemId, model);
    }

    /** Names a custom item model type for an item's definition, instead of {@code minecraft:model}. */
    public void itemModelType(Identifier itemId, String type) {
        itemTypes.put(itemId, type);
    }

    /**
     * Marks an item whose model reaches past its slot in GUIs; its definition gets
     * {@code "oversized_in_gui": true} so the slot icon is not cut at the 16 pixel slot.
     */
    public void oversizedInGui(Identifier itemId) {
        oversizedInGui.add(itemId);
    }

    /** Writes every model; item models of registered items also get their definition under {@code items/}. */
    CompletableFuture<?> write(CachedOutput cache, PackOutput output) {
        PackOutput.PathProvider models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
        PackOutput.PathProvider items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        generated.forEach((location, builder) -> {
            futures.add(DataProvider.saveStable(cache, builder.toJson(), models.json(location)));
            Identifier itemId = itemOf(location);
            if (itemId != null) {
                futures.add(DataProvider.saveStable(cache, itemDefinition(location, itemId), items.json(itemId)));
            }
        });
        rawItemModels.forEach((itemId, model) -> {
            JsonObject root = new JsonObject();
            root.add("model", model);
            futures.add(DataProvider.saveStable(cache, root, items.json(itemId)));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    /** The registered item whose own model sits at {@code location} ({@code item/<item>}), or null. */
    private @Nullable Identifier itemOf(Identifier location) {
        if (!folder.equals("item") || !location.getPath().startsWith("item/")) {
            return null;
        }
        Identifier itemId = location.withPath(location.getPath().substring("item/".length()));
        return !itemId.getPath().contains("/") && BuiltInRegistries.ITEM.containsKey(itemId) ? itemId : null;
    }

    /** {@code {"model": {"type": <type>, "model": "<location>"}}}, plus {@code oversized_in_gui} when marked. */
    private JsonObject itemDefinition(Identifier model, Identifier itemId) {
        JsonObject inner = new JsonObject();
        inner.addProperty("type", itemModelTypeOf(itemId));
        inner.addProperty("model", model.toString());
        JsonObject root = new JsonObject();
        root.add("model", inner);
        if (oversizedInGui.contains(itemId)) {
            root.addProperty("oversized_in_gui", true);
        }
        return root;
    }

    private String itemModelTypeOf(Identifier itemId) {
        if (itemTypes.containsKey(itemId)) {
            return itemTypes.get(itemId);
        }
        return Wood.isWoodBlock(itemId) ? "servus_foundation:wood" : "minecraft:model";
    }

    /** Item model folder with the {@code basicItem} helper. */
    public static class Items extends ModelProvider<ItemModelBuilder> {

        public Items(String modid) {
            super(modid, "item", ItemModelBuilder::new);
        }

        /** A flat item model with the texture {@code item/<name>}. */
        public ItemModelBuilder basicItem(Item item) {
            return basicItem(BuiltInRegistries.ITEM.getKey(item));
        }

        /** A flat item model with the texture {@code item/<name>}. */
        public ItemModelBuilder basicItem(Identifier itemId) {
            return getBuilder(itemId.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", Identifier.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath()));
        }
    }
}
