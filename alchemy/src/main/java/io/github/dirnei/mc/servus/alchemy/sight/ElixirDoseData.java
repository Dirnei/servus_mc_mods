package io.github.dirnei.mc.servus.alchemy.sight;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

/**
 * Loads how dangerous each elixir is from {@code data/<namespace>/servus_alchemy/elixir_dose/<name>.json}; the file id is
 * the elixir's item id.
 */
public final class ElixirDoseData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "elixir_dose_data");

    public static final String DIRECTORY = "servus_alchemy/elixir_dose";

    private static final Codec<Double> CHANCE = Codec.doubleRange(0, 1);
    private static final Codec<Integer> TICKS = Codec.intRange(0, Integer.MAX_VALUE);

    private static final Codec<ElixirDose> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CHANCE.fieldOf("poison_chance").forGetter(ElixirDose::poisonChance),
            CHANCE.optionalFieldOf("extra_chance_while_sighted", 0.0).forGetter(ElixirDose::extraChanceWhileSighted),
            TICKS.optionalFieldOf("poison_ticks", 0).forGetter(ElixirDose::poisonTicks),
            TICKS.optionalFieldOf("nausea_ticks", 0).forGetter(ElixirDose::nauseaTicks)
    ).apply(instance, ElixirDose::new));

    public static final ElixirDoseData INSTANCE = new ElixirDoseData();

    private volatile Map<Identifier, ElixirDose> doses = Map.of();

    private ElixirDoseData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, ElixirDose> loaded = new HashMap<>();
        files.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid elixir dose {}: {}", id, error))
                .ifPresent(dose -> loaded.put(id, dose)));
        doses = Map.copyOf(loaded);
        ServusAlchemy.LOGGER.info("Loaded {} elixir doses", loaded.size());
    }

    /** The dose of {@code elixir}, {@link ElixirDose#HARMLESS} when no file names it. */
    public ElixirDose forItem(Item elixir) {
        return doses.getOrDefault(BuiltInRegistries.ITEM.getKey(elixir), ElixirDose.HARMLESS);
    }
}
