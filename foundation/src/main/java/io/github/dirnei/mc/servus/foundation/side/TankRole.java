package io.github.dirnei.mc.servus.foundation.side;

/** Which way fluid may move through a tank from the sides of its block. */
public enum TankRole {
    /** Sides may fill the tank but never drain it. */
    FILL_ONLY,
    /** Sides may drain the tank but never fill it. */
    DRAIN_ONLY,
    /** Sides may fill and drain the tank. */
    ANY;

    public boolean fills() {
        return this != DRAIN_ONLY;
    }

    public boolean drains() {
        return this != FILL_ONLY;
    }
}
