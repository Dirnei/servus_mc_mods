package io.github.dirnei.mc.servus.alchemy.homunculus;

/**
 * The homunculus' numbers from datapack data. Pure Java.
 *
 * @param growTicks       fed ticks until a new homunculus is grown
 * @param workInterval    fed ticks between two harvests of a grown homunculus
 * @param elixirTicks     how many ticks one Elixir of Sight feeds it
 * @param giftBelowTicks  a gnome whose boost has fewer ticks left gets a gift
 * @param freedMultiplier items delivered per item taken once it is freed
 */
public record HomunculusSettings(int growTicks, int workInterval, int elixirTicks, int giftBelowTicks, int freedMultiplier) {

    /** The shipped example values: two in-game days to grow, a harvest every 5 seconds, one Elixir per 10 minutes. */
    public static final HomunculusSettings DEFAULT = new HomunculusSettings(48000, 100, 12000, 1200, 2);

    public HomunculusSettings {
        if (growTicks < 1 || workInterval < 1 || elixirTicks < 1 || giftBelowTicks < 0 || freedMultiplier < 1) {
            throw new IllegalArgumentException("invalid homunculus settings");
        }
    }
}
