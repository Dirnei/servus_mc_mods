package io.github.dirnei.mc.servus.brewery.rausch;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.dirnei.mc.servus.brewery.ServusBrewery;
import io.github.dirnei.mc.servus.brewery.ServusBreweryIds;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

/** Loads the Rausch balance numbers from {@code data/servus_brewery/rausch/stats.json}. */
public final class RauschStatsManager extends SimpleJsonResourceReloadListener<JsonElement> {

    /** Key of this listener in the reload event. */
    public static final Identifier LISTENER_ID = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "rausch_stats_manager");
    private static final Identifier STATS = Identifier.fromNamespaceAndPath(ServusBreweryIds.MOD_ID, "stats");

    private record PartialLevel(Optional<Integer> from, Optional<Double> luck, Optional<Double> armor,
            Optional<Double> fallDamage, Optional<Double> speed, Optional<Double> knockback, Optional<Boolean> wobble,
            Optional<Double> stumbleChance, Optional<Double> stumbleStrength) {

        static final Codec<PartialLevel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("from").forGetter(PartialLevel::from),
                Codec.DOUBLE.optionalFieldOf("luck").forGetter(PartialLevel::luck),
                Codec.DOUBLE.optionalFieldOf("armor").forGetter(PartialLevel::armor),
                Codec.DOUBLE.optionalFieldOf("fall_damage").forGetter(PartialLevel::fallDamage),
                Codec.DOUBLE.optionalFieldOf("speed").forGetter(PartialLevel::speed),
                Codec.DOUBLE.optionalFieldOf("knockback").forGetter(PartialLevel::knockback),
                Codec.BOOL.optionalFieldOf("wobble").forGetter(PartialLevel::wobble),
                Codec.DOUBLE.optionalFieldOf("stumble_chance").forGetter(PartialLevel::stumbleChance),
                Codec.DOUBLE.optionalFieldOf("stumble_strength").forGetter(PartialLevel::stumbleStrength)
        ).apply(instance, PartialLevel::new));

        LevelStats over(LevelStats defaults) {
            return new LevelStats(from.orElse(defaults.from()), luck.orElse(defaults.luck()), armor.orElse(defaults.armor()),
                    Math.max(0, fallDamage.orElse(defaults.fallDamage())), speed.orElse(defaults.speed()),
                    Math.max(0, knockback.orElse(defaults.knockback())), wobble.orElse(defaults.wobble()),
                    stumbleChance.orElse(defaults.stumbleChance()), stumbleStrength.orElse(defaults.stumbleStrength()));
        }
    }

    private record PartialHangover(Optional<Integer> from, Optional<Integer> duration, Optional<Double> miningSpeed,
            Optional<Double> exhaustion, Optional<Integer> hairOfTheDog) {

        static final Codec<PartialHangover> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("from").forGetter(PartialHangover::from),
                Codec.INT.optionalFieldOf("duration").forGetter(PartialHangover::duration),
                Codec.DOUBLE.optionalFieldOf("mining_speed").forGetter(PartialHangover::miningSpeed),
                Codec.DOUBLE.optionalFieldOf("exhaustion").forGetter(PartialHangover::exhaustion),
                Codec.INT.optionalFieldOf("hair_of_the_dog").forGetter(PartialHangover::hairOfTheDog)
        ).apply(instance, PartialHangover::new));

        HangoverStats over(HangoverStats defaults) {
            return new HangoverStats(from.orElse(defaults.from()), Math.max(1, duration.orElse(defaults.duration())),
                    miningSpeed.orElse(defaults.miningSpeed()), exhaustion.orElse(defaults.exhaustion()),
                    Math.max(0, hairOfTheDog.orElse(defaults.hairOfTheDog())));
        }
    }

    private record Partial(Optional<Integer> perDrink, Optional<Integer> decayTicks, Optional<Integer> max,
            Map<String, PartialLevel> levels, Optional<PartialHangover> hangover) {

        static final Codec<Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("per_drink").forGetter(Partial::perDrink),
                Codec.INT.optionalFieldOf("decay_ticks").forGetter(Partial::decayTicks),
                Codec.INT.optionalFieldOf("max").forGetter(Partial::max),
                Codec.unboundedMap(Codec.STRING, PartialLevel.CODEC).optionalFieldOf("levels", Map.of()).forGetter(Partial::levels),
                PartialHangover.CODEC.optionalFieldOf("hangover").forGetter(Partial::hangover)
        ).apply(instance, Partial::new));

        RauschStats over(RauschStats defaults) {
            Map<RauschLevel, LevelStats> merged = new EnumMap<>(RauschLevel.class);
            for (RauschLevel level : RauschLevel.values()) {
                PartialLevel partial = levels.get(level.key());
                merged.put(level, partial == null ? defaults.level(level) : partial.over(defaults.level(level)));
            }
            return new RauschStats(perDrink.orElse(defaults.perDrink()), decayTicks.orElse(defaults.decayTicks()),
                    max.orElse(defaults.max()), merged,
                    hangover.map(partial -> partial.over(defaults.hangover())).orElse(defaults.hangover()));
        }
    }

    public static final RauschStatsManager INSTANCE = new RauschStatsManager();

    private volatile RauschStats stats = RauschStats.DEFAULTS;

    private RauschStatsManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("rausch"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        JsonElement json = files.get(STATS);
        stats = json == null ? RauschStats.DEFAULTS : Partial.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> ServusBrewery.LOGGER.error("Invalid Rausch stats: {}", error))
                .map(partial -> partial.over(RauschStats.DEFAULTS))
                .orElse(RauschStats.DEFAULTS);
    }

    /** The loaded stats, or the defaults before the first load. */
    public RauschStats stats() {
        return stats;
    }
}
