package io.github.dirnei.mc.servus.manufacturing.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Balance numbers of one machine. Loaded from {@code data/<ns>/machine/<name>.json};
 * every field is optional in the file and falls back to the machine's code defaults.
 *
 * @param workTicks      ticks per work cycle
 * @param energyPerTick  FE consumed per tick while working (0 for burner machines)
 * @param energyCapacity FE buffer size (0 for burner machines)
 */
public record MachineStats(int workTicks, int energyPerTick, int energyCapacity) {

    /** Partial stats as written in JSON. */
    public record Partial(Optional<Integer> workTicks, Optional<Integer> energyPerTick, Optional<Integer> energyCapacity) {

        public static final Codec<Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("work_ticks").forGetter(Partial::workTicks),
                Codec.INT.optionalFieldOf("energy_per_tick").forGetter(Partial::energyPerTick),
                Codec.INT.optionalFieldOf("energy_capacity").forGetter(Partial::energyCapacity)
        ).apply(instance, Partial::new));

        public MachineStats over(MachineStats defaults) {
            return new MachineStats(
                    workTicks.orElse(defaults.workTicks()),
                    energyPerTick.orElse(defaults.energyPerTick()),
                    energyCapacity.orElse(defaults.energyCapacity()));
        }
    }

    public static MachineStats burner(int workTicks) {
        return new MachineStats(workTicks, 0, 0);
    }
}
