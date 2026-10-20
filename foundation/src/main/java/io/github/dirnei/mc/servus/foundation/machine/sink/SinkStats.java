package io.github.dirnei.mc.servus.foundation.machine.sink;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * Balance numbers of the Sink, read from {@code data/servus_foundation/sink/sink.json}.
 * Every field is optional and falls back to {@link #DEFAULTS}.
 *
 * @param rates        void rates per chimney height
 * @param activeTicks  how long the Sink stays active after it voided something
 * @param slotsPerRow  item slots in one buffer row, {@link #MIN_SLOTS_PER_ROW} to {@link #MAX_SLOTS_PER_ROW}
 * @param tankCapacity mB one buffer tank holds
 * @param upgradeSlots Capacity Upgrade slots; each upgrade opens one more row and tank
 */
public record SinkStats(SinkRates rates, int activeTicks, int slotsPerRow, int tankCapacity, int upgradeSlots) {

    /** The GUI has room for this many slots beside a tank. */
    public static final int MAX_SLOTS_PER_ROW = 7;
    public static final int MIN_SLOTS_PER_ROW = 1;

    public static final SinkStats DEFAULTS = new SinkStats(new SinkRates(
            List.of(4, 8, 16, 32, 64),
            List.of(1000, 2000, 4000, 8000, 16000),
            List.of(100, 200, 400, 800, 1600)), 40, 7, 8000, 3);

    public SinkStats {
        activeTicks = Math.max(1, activeTicks);
        slotsPerRow = Math.clamp(slotsPerRow, MIN_SLOTS_PER_ROW, MAX_SLOTS_PER_ROW);
        tankCapacity = Math.max(1, tankCapacity);
        upgradeSlots = Math.max(0, upgradeSlots);
    }

    public static final Codec<SinkStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.listOf().optionalFieldOf("items_per_second", DEFAULTS.rates().itemsPerSecond())
                    .forGetter(stats -> stats.rates().itemsPerSecond()),
            Codec.INT.listOf().optionalFieldOf("fluid_per_second", DEFAULTS.rates().fluidPerSecond())
                    .forGetter(stats -> stats.rates().fluidPerSecond()),
            Codec.INT.listOf().optionalFieldOf("energy_per_tick", DEFAULTS.rates().energyPerTick())
                    .forGetter(stats -> stats.rates().energyPerTick()),
            Codec.INT.optionalFieldOf("active_ticks", DEFAULTS.activeTicks()).forGetter(SinkStats::activeTicks),
            Codec.INT.optionalFieldOf("slots_per_row", DEFAULTS.slotsPerRow()).forGetter(SinkStats::slotsPerRow),
            Codec.INT.optionalFieldOf("tank_capacity", DEFAULTS.tankCapacity()).forGetter(SinkStats::tankCapacity),
            Codec.INT.optionalFieldOf("upgrade_slots", DEFAULTS.upgradeSlots()).forGetter(SinkStats::upgradeSlots)
    ).apply(instance, (items, fluid, energy, active, slotsPerRow, tankCapacity, upgradeSlots) ->
            new SinkStats(new SinkRates(items, fluid, energy), active, slotsPerRow, tankCapacity, upgradeSlots)));

    /** The buffer layout these stats give. */
    public SinkCapacity capacity() {
        return new SinkCapacity(slotsPerRow, upgradeSlots);
    }
}
