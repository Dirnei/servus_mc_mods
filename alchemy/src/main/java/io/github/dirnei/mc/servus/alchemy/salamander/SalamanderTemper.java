package io.github.dirnei.mc.servus.alchemy.salamander;

import java.util.Map;

/**
 * How a salamander takes to gold: the chance one Gold Ingot befriends a wild one, and how long a befriended one digests
 * an ingot before it gives back Sal Ammoniac; and where wild ones spawn.
 *
 * @param befriendChance chance from 0 to 1 that one ingot befriends
 * @param digestTicks    ticks from feeding to the Sal Ammoniac, at least 1
 * @param spawn          where and how many wild salamanders spawn
 */
public record SalamanderTemper(double befriendChance, int digestTicks, Spawn spawn) {

    /** The numbers used when no data file names the salamander. */
    public static final SalamanderTemper DEFAULT = new SalamanderTemper(0.33, 1200);

    public SalamanderTemper {
        if (!(befriendChance >= 0 && befriendChance <= 1)) {
            throw new IllegalArgumentException("befriendChance must be between 0 and 1, was " + befriendChance);
        }
        if (digestTicks < 1) {
            throw new IllegalArgumentException("digestTicks must be at least 1, was " + digestTicks);
        }
        if (spawn == null) {
            throw new IllegalArgumentException("spawn must not be null");
        }
    }

    /** A temper with the default spawn numbers. */
    public SalamanderTemper(double befriendChance, int digestTicks) {
        this(befriendChance, digestTicks, Spawn.DEFAULT);
    }

    /** Whether an ingot befriends for a {@code roll} from 0 (inclusive) to 1 (exclusive). */
    public boolean befriends(double roll) {
        return roll < befriendChance;
    }

    /**
     * Where and how many wild salamanders spawn.
     *
     * @param homeRadius  how far, as a box, a home block may be from a ground spawn spot; 0 to {@link #MAX_HOME_RADIUS}
     * @param crowdRadius how far around a spawn spot wild salamanders count against {@code crowdMax}, at least 0
     * @param crowdMax    a spawn is refused when this many wild salamanders already live within {@code crowdRadius}, at least 0
     * @param biomeChance chance from 0 to 1 per biome id that a spawn try passes; a biome without an entry always passes
     */
    public record Spawn(int homeRadius, int crowdRadius, int crowdMax, Map<String, Double> biomeChance) {

        /** The largest home radius: a ground spawn try reads a box of at most 9 by 9 by 9 blocks. */
        public static final int MAX_HOME_RADIUS = 4;

        /** The spawn numbers used when the data file has no spawn object. */
        public static final Spawn DEFAULT = new Spawn(4, 32, 3, Map.of("minecraft:warped_forest", 0.5));

        public Spawn {
            if (homeRadius < 0 || homeRadius > MAX_HOME_RADIUS) {
                throw new IllegalArgumentException("homeRadius must be between 0 and " + MAX_HOME_RADIUS + ", was " + homeRadius);
            }
            if (crowdRadius < 0) {
                throw new IllegalArgumentException("crowdRadius must be at least 0, was " + crowdRadius);
            }
            if (crowdMax < 0) {
                throw new IllegalArgumentException("crowdMax must be at least 0, was " + crowdMax);
            }
            biomeChance = Map.copyOf(biomeChance);
            biomeChance.forEach((biome, chance) -> {
                if (!(chance >= 0 && chance <= 1)) {
                    throw new IllegalArgumentException("biome chance of " + biome + " must be between 0 and 1, was " + chance);
                }
            });
        }

        /** Whether a spawn try in {@code biome} passes for a {@code roll} from 0 (inclusive) to 1 (exclusive). */
        public boolean biomeAllows(String biome, double roll) {
            return roll < biomeChance.getOrDefault(biome, 1.0);
        }
    }
}
