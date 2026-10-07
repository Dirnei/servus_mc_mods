package io.github.dirnei.mc.servus.storage.shuttle;

/**
 * How far a deck's flaps are folded up for the lift car. Server (may a
 * shuttle cross?) and client (how to draw the deck) use the same numbers.
 * Pure, no Minecraft imports.
 */
public final class Flaps {

    /** Car within this distance of a floor: the flaps there are fully up. */
    public static final double UP_WITHIN = 0.5;
    /** Car at least this far from a floor: the flaps there are fully down. */
    public static final double DOWN_FROM = 1.0;

    private Flaps() {
    }

    /** Flap angle in degrees: 90 is folded up against the shaft edge, 0 is down as a floor. */
    public static double angle(double carY, double floorY) {
        double distance = Math.abs(carY - floorY);
        if (distance <= UP_WITHIN) {
            return 90;
        }
        if (distance >= DOWN_FROM) {
            return 0;
        }
        return 90 * (DOWN_FROM - distance) / (DOWN_FROM - UP_WITHIN);
    }
}
