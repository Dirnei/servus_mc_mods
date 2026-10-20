package io.github.dirnei.mc.servus.delicacies.firebox;

/** Where an {@link ElectricHeat} takes its FE from. */
@FunctionalInterface
public interface EnergyDraw {

    /** Takes {@code amount} FE when that much is stored and returns true; otherwise takes nothing and returns false. */
    boolean draw(int amount);
}
