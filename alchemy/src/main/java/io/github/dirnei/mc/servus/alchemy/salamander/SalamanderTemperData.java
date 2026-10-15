package io.github.dirnei.mc.servus.alchemy.salamander;

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
import net.minecraft.world.entity.EntityType;

/**
 * Loads how a salamander takes to gold and where it spawns from
 * {@code data/<namespace>/servus_alchemy/salamander_temper/<name>.json}; the file id is the entity type id. The {@code spawn}
 * object and each of its fields may be left out for the defaults.
 */
public final class SalamanderTemperData extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusAlchemyIds.MOD_ID, "salamander_temper_data");

    public static final String DIRECTORY = "servus_alchemy/salamander_temper";

    private static final SalamanderTemper.Spawn SPAWN_DEFAULT = SalamanderTemper.Spawn.DEFAULT;

    private static final Codec<SalamanderTemper.Spawn> SPAWN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, SalamanderTemper.Spawn.MAX_HOME_RADIUS).optionalFieldOf("home_radius", SPAWN_DEFAULT.homeRadius())
                    .forGetter(SalamanderTemper.Spawn::homeRadius),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("crowd_radius", SPAWN_DEFAULT.crowdRadius())
                    .forGetter(SalamanderTemper.Spawn::crowdRadius),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("crowd_max", SPAWN_DEFAULT.crowdMax())
                    .forGetter(SalamanderTemper.Spawn::crowdMax),
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0, 1)).optionalFieldOf("biome_chance", SPAWN_DEFAULT.biomeChance())
                    .forGetter(SalamanderTemper.Spawn::biomeChance)
    ).apply(instance, SalamanderTemper.Spawn::new));

    private static final Codec<SalamanderTemper> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1).fieldOf("befriend_chance").forGetter(SalamanderTemper::befriendChance),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("digest_ticks").forGetter(SalamanderTemper::digestTicks),
            SPAWN_CODEC.optionalFieldOf("spawn", SPAWN_DEFAULT).forGetter(SalamanderTemper::spawn)
    ).apply(instance, SalamanderTemper::new));

    public static final SalamanderTemperData INSTANCE = new SalamanderTemperData();

    private volatile Map<Identifier, SalamanderTemper> tempers = Map.of();

    private SalamanderTemperData() {
        super(ExtraCodecs.JSON, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, SalamanderTemper> loaded = new HashMap<>();
        files.forEach((id, json) -> CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusAlchemy.LOGGER.error("Invalid salamander temper {}: {}", id, error))
                .ifPresent(temper -> loaded.put(id, temper)));
        tempers = Map.copyOf(loaded);
        ServusAlchemy.LOGGER.info("Loaded {} salamander tempers", loaded.size());
    }

    /** The temper of {@code type}, {@link SalamanderTemper#DEFAULT} when no file names it. */
    public SalamanderTemper forType(EntityType<?> type) {
        return tempers.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type), SalamanderTemper.DEFAULT);
    }
}
