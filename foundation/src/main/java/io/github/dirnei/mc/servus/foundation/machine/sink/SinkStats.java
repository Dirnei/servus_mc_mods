package io.github.dirnei.mc.servus.foundation.machine.sink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * Balance numbers of the Sink, read from {@code data/servus_foundation/sink/sink.json}.
 * Every field is optional and falls back to {@link #DEFAULTS}.
 *
 * @param rates       void rates per chimney height
 * @param activeTicks how long the Sink stays active after it voided something
 */
public record SinkStats(SinkRates rates, int activeTicks) {

    public static final SinkStats DEFAULTS = new SinkStats(new SinkRates(
            List.of(4, 8, 16, 32, 64),
            List.of(50, 100, 200, 400, 800),
            List.of(100, 200, 400, 800, 1600)), 40);

    public static final Codec<SinkStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().optionalFieldOf("items_per_second", DEFAULTS.rates().itemsPerSecond())
                    .forGetter(stats -> stats.rates().itemsPerSecond()),
            Codec.INT.listOf().optionalFieldOf("fluid_per_tick", DEFAULTS.rates().fluidPerTick())
                    .forGetter(stats -> stats.rates().fluidPerTick()),
            Codec.INT.listOf().optionalFieldOf("energy_per_tick", DEFAULTS.rates().energyPerTick())
                    .forGetter(stats -> stats.rates().energyPerTick()),
            Codec.INT.optionalFieldOf("active_ticks", DEFAULTS.activeTicks()).forGetter(SinkStats::activeTicks)
    ).apply(instance, (items, fluid, energy, active) -> new SinkStats(new SinkRates(items, fluid, energy), Math.max(1, active))));
}
