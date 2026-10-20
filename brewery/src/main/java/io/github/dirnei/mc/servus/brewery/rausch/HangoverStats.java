package io.github.dirnei.mc.servus.brewery.rausch;

/**
 * The Hangover's numbers. Pure Java.
 *
 * @param from         the Rausch points that make a Hangover due the next morning
 * @param duration     ticks the Hangover lasts
 * @param miningSpeed  mining speed change as a fraction: -0.3 is 30% slower
 * @param exhaustion   food exhaustion added per second
 * @param hairOfTheDog the Rausch points a beer sets while hungover
 */
public record HangoverStats(int from, int duration, double miningSpeed, double exhaustion, int hairOfTheDog) {
}
