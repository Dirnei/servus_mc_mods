package io.github.dirnei.mc.servus.manufacturing.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Balance numbers of one machine. Loaded from {@code data/<ns>/machine/<name>.json};
 * every field is optional in the file and falls back to the machine's code defaults.
 *
 * @param workTicks      ticks per work cycle
 * @param energyPerTick  FE consumed (machines) or produced (generators) per tick
 * @param energyCapacity FE buffer size (0 for burner machines)
 * @param fuelDivisor    furnace burn time is divided by this (generators burn faster)
 */
public record MachineStats(int workTicks, int energyPerTick, int energyCapacity, int fuelDivisor,
                           int itemsPerTick, int recipeSlots) {

    /** Partial stats as written in JSON. */
    public record Partial(Optional<Integer> workTicks, Optional<Integer> energyPerTick,
                          Optional<Integer> energyCapacity, Optional<Integer> fuelDivisor,
                          Optional<Integer> itemsPerTick, Optional<Integer> recipeSlots) {

        public static final Codec<Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("work_ticks").forGetter(Partial::workTicks),
                Codec.INT.optionalFieldOf("energy_per_tick").forGetter(Partial::energyPerTick),
                Codec.INT.optionalFieldOf("energy_capacity").forGetter(Partial::energyCapacity),
                Codec.INT.optionalFieldOf("fuel_divisor").forGetter(Partial::fuelDivisor),
                Codec.INT.optionalFieldOf("items_per_tick").forGetter(Partial::itemsPerTick),
                Codec.INT.optionalFieldOf("recipe_slots").forGetter(Partial::recipeSlots)
        ).apply(instance, Partial::new));

        public MachineStats over(MachineStats defaults) {
            return new MachineStats(
                    workTicks.orElse(defaults.workTicks()),
                    energyPerTick.orElse(defaults.energyPerTick()),
                    energyCapacity.orElse(defaults.energyCapacity()),
                    Math.max(1, fuelDivisor.orElse(defaults.fuelDivisor())),
                    itemsPerTick.orElse(defaults.itemsPerTick()),
                    Math.max(1, recipeSlots.orElse(defaults.recipeSlots())));
        }
    }

    public static MachineStats burner(int workTicks) {
        return new MachineStats(workTicks, 0, 0, 1, 0, 1);
    }

    public static MachineStats powered(int workTicks, int energyPerTick, int energyCapacity) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, 1);
    }

    public static MachineStats crafter(int workTicks, int energyPerTick, int energyCapacity, int recipeSlots) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, recipeSlots);
    }

    public static MachineStats generator(int energyPerTick, int energyCapacity, int fuelDivisor) {
        return new MachineStats(1, energyPerTick, energyCapacity, fuelDivisor, 0, 1);
    }
}
