package io.github.dirnei.mc.servus.alchemy.salamander;

/**
 * How a salamander takes to gold: the chance one Gold Ingot befriends a wild one, and how long a befriended one digests
 * an ingot before it gives back Sal Ammoniac.
 *
 * @param befriendChance chance from 0 to 1 that one ingot befriends
 * @param digestTicks    ticks from feeding to the Sal Ammoniac, at least 1
 */
public record SalamanderTemper(double befriendChance, int digestTicks) {

    /** The numbers used when no data file names the salamander. */
    public static final SalamanderTemper DEFAULT = new SalamanderTemper(0.33, 1200);

    public SalamanderTemper {
        if (!(befriendChance >= 0 && befriendChance <= 1)) {
            throw new IllegalArgumentException("befriendChance must be between 0 and 1, was " + befriendChance);
        }
        if (digestTicks < 1) {
            throw new IllegalArgumentException("digestTicks must be at least 1, was " + digestTicks);
        }
    }

    /** Whether an ingot befriends for a {@code roll} from 0 (inclusive) to 1 (exclusive). */
    public boolean befriends(double roll) {
        return roll < befriendChance;
    }
}
