package io.github.dirnei.mc.servus.alchemy.hint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * How the old gnome wakes and shows himself.
 *
 * @param wakeChance    chance per fitting broken block to wake him
 * @param wakeChapter   journal chapter the team must have finished
 * @param maxY          only blocks below this height wake him
 * @param knockRange    a player within this many blocks hears him knock
 * @param knockInterval ticks between two knocking series
 * @param dustRange     a player within this many blocks sees dust fall
 * @param traceRange    blocks broken within this many blocks of him may drop a trace
 * @param traceChance   chance per such block to drop a trace
 */
public record OldGnomeSettings(double wakeChance, String wakeChapter, int maxY, int knockRange, int knockInterval, int dustRange,
                               int traceRange, double traceChance) {

    public static final OldGnomeSettings DEFAULTS = new OldGnomeSettings(0.003, "servus_alchemy:schwaz", 0, 24, 100, 6, 8, 0.15);

    private static final Codec<Double> CHANCE = Codec.doubleRange(0, 1);
    private static final Codec<Integer> POSITIVE = Codec.intRange(1, Integer.MAX_VALUE);

    public static final Codec<OldGnomeSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CHANCE.optionalFieldOf("wake_chance", DEFAULTS.wakeChance()).forGetter(OldGnomeSettings::wakeChance),
            Codec.STRING.optionalFieldOf("wake_chapter", DEFAULTS.wakeChapter()).forGetter(OldGnomeSettings::wakeChapter),
            Codec.INT.optionalFieldOf("max_y", DEFAULTS.maxY()).forGetter(OldGnomeSettings::maxY),
            POSITIVE.optionalFieldOf("knock_range", DEFAULTS.knockRange()).forGetter(OldGnomeSettings::knockRange),
            POSITIVE.optionalFieldOf("knock_interval", DEFAULTS.knockInterval()).forGetter(OldGnomeSettings::knockInterval),
            POSITIVE.optionalFieldOf("dust_range", DEFAULTS.dustRange()).forGetter(OldGnomeSettings::dustRange),
            POSITIVE.optionalFieldOf("trace_range", DEFAULTS.traceRange()).forGetter(OldGnomeSettings::traceRange),
            CHANCE.optionalFieldOf("trace_chance", DEFAULTS.traceChance()).forGetter(OldGnomeSettings::traceChance)
    ).apply(instance, OldGnomeSettings::new));

    /** The same settings with another wake chance and height, for tests. */
    public OldGnomeSettings withWake(double chance, int belowY) {
        return new OldGnomeSettings(chance, wakeChapter, belowY, knockRange, knockInterval, dustRange, traceRange, traceChance);
    }

    /** The same settings with another trace chance, for tests. */
    public OldGnomeSettings withTraceChance(double chance) {
        return new OldGnomeSettings(wakeChance, wakeChapter, maxY, knockRange, knockInterval, dustRange, traceRange, chance);
    }
}
