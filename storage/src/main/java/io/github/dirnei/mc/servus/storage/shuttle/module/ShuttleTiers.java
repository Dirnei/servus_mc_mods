package io.github.dirnei.mc.servus.storage.shuttle.module;

import io.github.dirnei.mc.servus.storage.shuttle.module.build.BuildStep;
import java.util.List;

/**
 * The shuttle stats from {@code storage/shuttle.json}: speed, stop times and
 * the tiers with their stacks, upgrade maxes and build sheets. Counts are
 * always clamped to the tier's max. Pure, no Minecraft imports.
 *
 * @param baseSpeed      blocks per tick without Speed upgrades
 * @param speedPerCount  blocks per tick each Speed upgrade adds
 * @param stopTicks      ticks every loading or unloading stop takes
 * @param stackTicks     ticks a stop takes per stack moved there
 * @param handlingFactor what each Handling upgrade multiplies stop and work times by
 * @param tiers          Mk1 first; at least one
 */
public record ShuttleTiers(double baseSpeed, double speedPerCount, int stopTicks, int stackTicks, double handlingFactor, List<Tier> tiers) {

    /** The upgrades counted on a shuttle. */
    public enum Kind { SPEED, HANDLING }

    /** One supply of a build sheet: an item id and how many. */
    public record Supply(String id, int count) {
    }

    /**
     * One tier.
     *
     * @param build the supplies that make this tier from the one below; empty for Mk1
     * @param steps the steps of a build by hand of this tier; a tier with a build and no steps gets {@link BuildStep#DEFAULTS}
     */
    public record Tier(int stacks, int maxSpeed, int maxHandling, List<Supply> build, List<BuildStep> steps) {

        public Tier {
            stacks = Math.max(1, stacks);
            maxSpeed = Math.max(0, maxSpeed);
            maxHandling = Math.max(0, maxHandling);
            build = List.copyOf(build);
            steps = build.isEmpty() ? List.of() : steps.isEmpty() ? BuildStep.DEFAULTS : List.copyOf(steps);
        }

        /** A tier whose build by hand has the default steps. */
        public Tier(int stacks, int maxSpeed, int maxHandling, List<Supply> build) {
            this(stacks, maxSpeed, maxHandling, build, List.of());
        }

        public int max(Kind kind) {
            return kind == Kind.SPEED ? maxSpeed : maxHandling;
        }
    }

    public static final ShuttleTiers DEFAULT = new ShuttleTiers(0.15, 0.04, 10, 4, 0.8, List.of(
            new Tier(2, 2, 2, List.of()),
            new Tier(4, 4, 4, List.of(new Supply("minecraft:gold_ingot", 16), new Supply("minecraft:chest", 2),
                    new Supply("minecraft:redstone", 16))),
            new Tier(8, 8, 8, List.of(new Supply("minecraft:diamond", 8), new Supply("minecraft:gold_block", 2),
                    new Supply("minecraft:ender_chest", 1)))));

    public ShuttleTiers {
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("at least one tier");
        }
        tiers = List.copyOf(tiers);
        baseSpeed = Math.max(0.01, baseSpeed);
        speedPerCount = Math.max(0, speedPerCount);
        stopTicks = Math.max(0, stopTicks);
        stackTicks = Math.max(0, stackTicks);
        handlingFactor = Math.max(0.01, Math.min(1, handlingFactor));
    }

    /** The highest tier there is. */
    public int topTier() {
        return tiers.size();
    }

    /** Tier 1 to {@link #topTier()}; other numbers clamp to the nearest. */
    public Tier tier(int tier) {
        return tiers.get(Math.max(1, Math.min(tiers.size(), tier)) - 1);
    }

    /** A count kept within 0 and the tier's max for that kind. */
    public int clamp(int tier, Kind kind, int count) {
        return Math.max(0, Math.min(tier(tier).max(kind), count));
    }

    /** Blocks per tick of a shuttle of that tier with that many Speed upgrades. */
    public double speed(int tier, int count) {
        return baseSpeed + speedPerCount * clamp(tier, Kind.SPEED, count);
    }

    /** The factor that many Handling upgrades put on stop and work times. */
    public double factor(int tier, int count) {
        return Math.pow(handlingFactor, clamp(tier, Kind.HANDLING, count));
    }

    /** Ticks a stop that moves that many stacks takes at that factor, at least one. */
    public int stopTicks(double factor, int stacks) {
        return Math.max(1, (int) Math.round((stopTicks + stackTicks * Math.max(0, stacks)) * factor));
    }

    /** Ticks one worked plot takes at that factor, from the field's base work time, at least one. */
    public static int workTicks(int base, double factor) {
        return Math.max(1, (int) Math.round(base * factor));
    }

    /** How many more upgrades of that kind a shuttle of that tier with that count still takes. */
    public int room(int tier, Kind kind, int current) {
        return Math.max(0, tier(tier).max(kind) - current);
    }

    /** The build sheet that makes the tier above, or empty at the top tier. */
    public List<Supply> nextBuild(int tier) {
        return tier >= topTier() ? List.of() : tier(tier + 1).build();
    }

    /** The steps of a build by hand into the tier above, or empty at the top tier. */
    public List<BuildStep> nextSteps(int tier) {
        return tier >= topTier() ? List.of() : tier(tier + 1).steps();
    }
}
