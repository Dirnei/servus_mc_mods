package io.github.dirnei.mc.servus.foundation.machine;

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
 * @param itemCapacity   items a machine's output buffer holds (0 for machines without one)
 * @param restartBelow   fraction of its buffer a machine with a restart threshold waits for after its buffer filled
 *                       (1 restarts as soon as there is room)
 */
public record MachineStats(int workTicks, int energyPerTick, int energyCapacity, int fuelDivisor,
                           int itemsPerTick, int recipeSlots, int fuelReserve, int fluidPerCycle,
                           int fluidCapacity, int minSourceNeighbours, String upkeepItem, int upkeepLevel,
                           int itemCapacity, double restartBelow) {

    /** Upkeep of the burner machines unless their stats say otherwise. */
    public static final String DEFAULT_UPKEEP_ITEM = "minecraft:coal";
    public static final int DEFAULT_UPKEEP_LEVEL = 16;
    /** Restart threshold of a machine that runs again as soon as its buffer has room. */
    public static final double NO_RESTART_THRESHOLD = 1.0;
    /** Restart threshold of the generator and the pumps unless their stats say otherwise. */
    public static final double DEFAULT_RESTART_BELOW = 0.5;

    /** Stats without a restart threshold. */
    public MachineStats(int workTicks, int energyPerTick, int energyCapacity, int fuelDivisor, int itemsPerTick,
                        int recipeSlots, int fuelReserve, int fluidPerCycle, int fluidCapacity, int minSourceNeighbours,
                        String upkeepItem, int upkeepLevel, int itemCapacity) {
        this(workTicks, energyPerTick, energyCapacity, fuelDivisor, itemsPerTick, recipeSlots, fuelReserve, fluidPerCycle,
                fluidCapacity, minSourceNeighbours, upkeepItem, upkeepLevel, itemCapacity, NO_RESTART_THRESHOLD);
    }

    /** A restart threshold from data as a fraction between 0 and 1. */
    public static double restartFraction(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    /** Whether the machine keeps an upkeep item stocked. */
    public boolean hasUpkeep() {
        return !upkeepItem.isEmpty() && upkeepLevel > 0;
    }

    /** Partial stats as written in JSON. */
    public record Partial(Optional<Integer> workTicks, Optional<Integer> energyPerTick,
                          Optional<Integer> energyCapacity, Optional<Integer> fuelDivisor,
                          Optional<Integer> itemsPerTick, Optional<Integer> recipeSlots,
                          Optional<Integer> fuelReserve, Optional<Integer> fluidPerCycle,
                          Optional<Integer> fluidCapacity, Optional<Integer> minSourceNeighbours,
                          Optional<String> upkeepItem, Optional<Integer> upkeepLevel,
                          Optional<Integer> itemCapacity, Optional<Double> restartBelow) {

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
                Codec.INT.optionalFieldOf("min_source_neighbours").forGetter(Partial::minSourceNeighbours),
                Codec.STRING.optionalFieldOf("upkeep_item").forGetter(Partial::upkeepItem),
                Codec.INT.optionalFieldOf("upkeep_level").forGetter(Partial::upkeepLevel),
                Codec.INT.optionalFieldOf("item_capacity").forGetter(Partial::itemCapacity),
                Codec.DOUBLE.optionalFieldOf("restart_below").forGetter(Partial::restartBelow)
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
                    minSourceNeighbours.orElse(defaults.minSourceNeighbours()),
                    upkeepItem.orElse(defaults.upkeepItem()),
                    Math.max(0, upkeepLevel.orElse(defaults.upkeepLevel())),
                    Math.max(0, itemCapacity.orElse(defaults.itemCapacity())),
                    restartFraction(restartBelow.orElse(defaults.restartBelow())));
        }
    }

    public static MachineStats burner(int workTicks) {
        return burner(workTicks, 0);
    }

    /** A burner that keeps {@code fuelReserve} of its own yield as fuel. */
    public static MachineStats burner(int workTicks, int fuelReserve) {
        return new MachineStats(workTicks, 0, 0, 1, 0, 1, fuelReserve, 0, 0, 0, DEFAULT_UPKEEP_ITEM, DEFAULT_UPKEEP_LEVEL, 0);
    }

    public static MachineStats powered(int workTicks, int energyPerTick, int energyCapacity) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, 1, 0, 0, 0, 0, "", 0, 0);
    }

    public static MachineStats crafter(int workTicks, int energyPerTick, int energyCapacity, int recipeSlots) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, recipeSlots, 0, 0, 0, 0, "", 0, 0);
    }

    /** A driven machine with one tank of {@code fluidCapacity} mB and no balance numbers of its own besides. */
    public static MachineStats fluidHolder(int fluidCapacity) {
        return new MachineStats(1, 0, 0, 1, 0, 1, 0, 0, fluidCapacity, 0, "", 0, 0);
    }

    public static MachineStats burnerPump(int workTicks, int fluidPerCycle, int fluidCapacity, int minSourceNeighbours) {
        return new MachineStats(workTicks, 0, 0, 1, 0, 1, 0, fluidPerCycle, fluidCapacity, minSourceNeighbours,
                DEFAULT_UPKEEP_ITEM, DEFAULT_UPKEEP_LEVEL, 0, DEFAULT_RESTART_BELOW);
    }

    public static MachineStats poweredPump(int workTicks, int energyPerTick, int energyCapacity, int fluidPerCycle,
                                           int fluidCapacity, int minSourceNeighbours) {
        return new MachineStats(workTicks, energyPerTick, energyCapacity, 1, 0, 1, 0, fluidPerCycle, fluidCapacity,
                minSourceNeighbours, "", 0, 0, DEFAULT_RESTART_BELOW);
    }

    public static MachineStats generator(int energyPerTick, int energyCapacity, int fuelDivisor) {
        return new MachineStats(1, energyPerTick, energyCapacity, fuelDivisor, 0, 1, 0, 0, 0, 0, DEFAULT_UPKEEP_ITEM, DEFAULT_UPKEEP_LEVEL, 0,
                DEFAULT_RESTART_BELOW);
    }
}
