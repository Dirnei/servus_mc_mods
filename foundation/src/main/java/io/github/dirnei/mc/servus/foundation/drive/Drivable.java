package io.github.dirnei.mc.servus.foundation.drive;

/**
 * A block a Hand Crank, an Electric Motor or another driver turns. Registered
 * as the {@code servus_foundation:drive} block capability; the context is the
 * side the driver touches. Drivers call it on the server thread only.
 */
public interface Drivable {

    /** Whether work given now would be used; drivers spend nothing while this is false. */
    boolean wantsWork();

    /** Gives the block {@code work} units; a block that wants no work ignores it. */
    void addWork(int work);

    /**
     * Called every tick a driver turns the block, with or without work, so the
     * block can show that it turns. {@link #addWork} counts as a turn as well.
     */
    default void turn() {
    }
}
