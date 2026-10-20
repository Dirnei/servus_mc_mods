package io.github.dirnei.mc.servus.delicacies.firebox;

/** Where a {@link FuelFire} takes its next fuel item from. */
@FunctionalInterface
public interface FuelSupply {

    /** Takes one fuel item and returns how many ticks it burns, or 0 when there is no fuel. */
    int burnNext();
}
