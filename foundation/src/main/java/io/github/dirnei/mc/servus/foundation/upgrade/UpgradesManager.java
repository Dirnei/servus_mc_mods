package io.github.dirnei.mc.servus.foundation.upgrade;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.dirnei.mc.servus.foundation.ServusFoundation;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

/**
 * Loads upgrade multipliers from {@code data/<ns>/data/upgrades.json}: an
 * object keyed by item id with {@code speed_multiplier}. Files of every
 * namespace are merged, so other mods add their own upgrades.
 */
public final class UpgradesManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "upgrades_manager");

    public static final UpgradesManager INSTANCE = new UpgradesManager();

    private static final String FILE = "upgrades";

    private volatile Map<Identifier, Double> multipliers = Map.of();

    private UpgradesManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("data"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, Double> read = new HashMap<>();
        files.forEach((file, element) -> {
            if (!file.getPath().equals(FILE) || !(element instanceof JsonObject json)) {
                return;
            }
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getKey());
                if (id == null || !(entry.getValue() instanceof JsonObject stats) || !stats.has("speed_multiplier")) {
                    ServusFoundation.LOGGER.warn("Upgrades: skipping entry {} in {}", entry.getKey(), file);
                    continue;
                }
                read.put(id, stats.get("speed_multiplier").getAsDouble());
            }
        });
        multipliers = Map.copyOf(read);
        ServusFoundation.LOGGER.info("Upgrades: {}", multipliers);
    }

    /** The multiplier a datapack gives the item, null when none does. */
    public @Nullable Double multiplier(Identifier id) {
        return multipliers.get(id);
    }

    /** For tests: sets a multiplier as if a datapack had been loaded. */
    public void set(Identifier id, double multiplier) {
        edit(copy -> copy.put(id, multiplier));
    }

    /** For tests: removes a multiplier set with {@link #set}. */
    public void remove(Identifier id) {
        edit(copy -> copy.remove(id));
    }

    private void edit(Consumer<Map<Identifier, Double>> change) {
        Map<Identifier, Double> copy = new HashMap<>(multipliers);
        change.accept(copy);
        multipliers = Map.copyOf(copy);
    }
}
