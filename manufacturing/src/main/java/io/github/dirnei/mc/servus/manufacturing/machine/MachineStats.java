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
 * @param fuelReserve    how many of its own yield a machine keeps in its fuel slot when the yield is a fuel (0 disables)
 * @param fluidPerCycle  mB a pump adds to its buffer per work cycle (0 for other machines)
 * @param fluidCapacity  mB a pump's buffer holds (0 for other machines)
 * @param minSourceNeighbours water source neighbours the source below a pump needs (0 for other machines)
 */
public record MachineStats(int workTicks, int energyPerTick, int energyCapacity, int fuelDivisor,
                           int itemsPerTick, int recipeSlots, int fuelReserve, int fluidPerCycle,
                           int fluidCapacity, int minSourceNeighbours) {

    /** Partial stats as written in JSON. */
    public record Partial(Optional<Integer> workTicks, Optional<Integer> energyPerTick,
                          Optional<Integer> energyCapacity, Optional<Integer> fuelDivisor,
                          Optional<Integer> itemsPerTick, Optional<Integer> recipeSlots,
                          Optional<Integer> fuelReserve, Optional<Integer> fluidPerCycle,
                          Optional<Integer> fluidCapacity, Optional<Integer> minSourceNeighbours) {

        public static final Codec<Partial> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("work_ticks").forGetter(Partial::workTicks),
                Codec.INT.optionalFieldOf("energy_per_tick").forGetter(Partial::energyPerTick),
                Codec.INT.optionalFieldOf("energy_capacity").forGetter(Partial::energyCapacity),
                Codec.INT.optionalFieldOf("fuel_divisor").forGetter(Partial::fuelDivisor),
                Codec.INT.optionalFieldOf("items_per_tick").forGetter(Partial::itemsPerTick),
                Codec.INT.optionalFieldOf("recipe_slots").forGetter(Partial::recipeSlots),
                Codec.INT.optionalFieldOf("fuel_reserve").forGetter(Partial::fuelReserve),
                Codec.INT.optionalFieldOf("fluid_per_cycle").forGetter(Partial::fluidPerCycle),
                Codec.INT.optionalFieldOf("fluid_capacity").forGetter(Partial::fluidCapacity),
                Codec.INT.optionalFieldOf("min_source_neighbours").forGetter(Partial::minSourceNeighbours)
        ).apply(instance, Partial::new));

        public MachineStats over(MachineStats defaults) {
            return new MachineStats(
                    workTicks.orElse(defaults.workTicks()),
                    energyPerTick.orElse(defaults.energyPerTick()),
                    energyCapacity.orElse(defaults.energyCapacity()),
                    Math.max(1, fuelDivisor.orElse(defaults.fuelDivisor())),
                    itemsPerTick.orElse(defaults.itemsPerTick()),
                    Math.max(1, recipeSlots.orElse(defaults.recipeSlots())),
                    Math.max(0, fuelReserve.orElse(defaults.fuelReserve())),
                    fluidPerCycle.orElse(defaults.fluidPerCycle()),
                    fluidCapacity.orElse(defaults.fluidCapacity()),
                    minSourceNeighbours.orElse(defaults.minSourceNeighbours()));
        }
    }

    public static MachineStats burner(int workTicks) {
        return burner(workTicks, 0);
    }

    /** A burner that keeps {@code fuelReserve} of its own yield as fuel. */
    public static MachineStats burner(int workTicks, int fuelReserve) {
        return new MachineStats(workTicks, 0, 0, 1, 0, 1, fuelReserve, 0, 0, 0);
    }

    public static MachineStats powered(int workTicks, int energyPerTick, int energyCapacity) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, 1, 0, 0, 0, 0);
    }

    public static MachineStats crafter(int workTicks, int energyPerTick, int energyCapacity, int recipeSlots) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, recipeSlots, 0, 0, 0, 0);
    }

    public static MachineStats burnerPump(int workTicks, int fluidPerCycle, int fluidCapacity, int minSourceNeighbours) {
        return new MachineStats(workTicks, 0, 0, 1, 0, 1, 0, fluidPerCycle, fluidCapacity, minSourceNeighbours);
    }

    public static MachineStats poweredPump(int workTicks, int energyPerTick, int energyCapacity, int fluidPerCycle,
                                           int fluidCapacity, int minSourceNeighbours) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, 1, 0, fluidPerCycle, fluidCapacity,
                minSourceNeighbours);
    }

    public static MachineStats generator(int energyPerTick, int energyCapacity, int fuelDivisor) {
        return new MachineStats(1, energyPerTick, energyCapacity, fuelDivisor, 0, 1, 0, 0, 0, 0);
    }
}
