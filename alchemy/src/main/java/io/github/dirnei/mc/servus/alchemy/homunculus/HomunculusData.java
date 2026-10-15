package io.github.dirnei.mc.servus.alchemy.homunculus;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemy;
import io.github.dirnei.mc.servus.alchemy.ServusAlchemyIds;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Loads the homunculus' numbers from {@code data/<namespace>/servus_alchemy/homunculus/<name>.json}: the file
 * {@code servus_alchemy:homunculus} wins, else the first by id; without a file the shipped defaults apply.
 */
public final class HomunculusData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "homunculus_data");

    public static final String DIRECTORY = "servus_alchemy/homunculus";

    private static final Identifier MAIN = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "homunculus");
    private static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);
    private static final HomunculusSettings DEFAULT = HomunculusSettings.DEFAULT;

    private static final Codec<HomunculusSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            POSITIVE.optionalFieldOf("grow_ticks", DEFAULT.growTicks()).forGetter(HomunculusSettings::growTicks),
            POSITIVE.optionalFieldOf("work_interval", DEFAULT.workInterval()).forGetter(HomunculusSettings::workInterval),
            POSITIVE.optionalFieldOf("elixir_ticks", DEFAULT.elixirTicks()).forGetter(HomunculusSettings::elixirTicks),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("gift_below_ticks", DEFAULT.giftBelowTicks())
                    .forGetter(HomunculusSettings::giftBelowTicks),
            Codec.intRange(1, 64).optionalFieldOf("freed_multiplier", DEFAULT.freedMultiplier()).forGetter(HomunculusSettings::freedMultiplier)
    ).apply(instance, HomunculusSettings::new));

    public static final HomunculusData INSTANCE = new HomunculusData();

    private volatile HomunculusSettings settings = DEFAULT;

    private HomunculusData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, JsonElement> sorted = new TreeMap<>(files);
        JsonElement chosen = files.containsKey(MAIN) ? files.get(MAIN) : sorted.values().stream().findFirst().orElse(null);
        settings = chosen == null ? DEFAULT : CODEC.parse(JsonOps.INSTANCE, chosen)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid homunculus data: {}", error))
                .orElse(DEFAULT);
    }

    public HomunculusSettings settings() {
        return settings;
    }
}
