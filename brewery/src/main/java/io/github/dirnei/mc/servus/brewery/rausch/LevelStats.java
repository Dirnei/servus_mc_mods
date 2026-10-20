package io.github.dirnei.mc.servus.brewery.rausch;

/**
 * What one Rausch level does. Pure Java.
 *
 * @param from            the Rausch points from which the level applies
 * @param luck            luck added
 * @param armor           armor points added
 * @param fallDamage      factor on fall damage: 1 unchanged, 0.5 half, 0 none
 * @param speed           walking speed change as a fraction: -0.15 is 15% slower
 * @param knockback       factor on the knockback the player takes
 * @param wobble          whether the screen wobbles
 * @param stumbleChance   chance per second of a sideways stumble
 * @param stumbleStrength how hard a stumble pushes
 */
public record LevelStats(int from, double luck, double armor, double fallDamage, double speed, double knockback,
        boolean wobble, double stumbleChance, double stumbleStrength) {

    /** A level from {@code from} points with only luck and armor. */
    public static LevelStats mild(int from, double luck, double armor) {
        return new LevelStats(from, luck, armor, 1, 0, 1, false, 0, 0);
    }
}
