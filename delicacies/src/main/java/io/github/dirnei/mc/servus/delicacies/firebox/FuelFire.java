package io.github.dirnei.mc.servus.delicacies.firebox;

/**
 * A wood fire. Pure Java. It lights the next fuel item only while a spot asks for heat, is hot at once, and burns a
 * lit item down to the end like a furnace, also after the demand stops. When the fuel ends it is cold at once.
 */
public final class FuelFire implements HeatSource {

    private final FuelSupply supply;
    private int burnLeft;
    private int burnTotal;

    public FuelFire(FuelSupply supply) {
        this.supply = supply;
    }

    @Override
    public void tick(boolean demanded) {
        if (burnLeft > 0) {
            burnLeft--;
        }
        if (burnLeft == 0 && demanded) {
            int ticks = supply.burnNext();
            if (ticks > 0) {
                burnLeft = ticks;
                burnTotal = ticks;
            }
        }
    }

    @Override
    public boolean hot() {
        return burnLeft > 0;
    }

    @Override
    public float heat() {
        return hot() ? 1 : 0;
    }

    /** Ticks the burning item still burns. */
    public int burnLeft() {
        return burnLeft;
    }

    /** Ticks the burning item burns in all. */
    public int burnTotal() {
        return burnTotal;
    }

    /** Restores a saved fire. */
    public void restore(int burnLeft, int burnTotal) {
        this.burnLeft = Math.max(0, burnLeft);
        this.burnTotal = Math.max(this.burnLeft, burnTotal);
    }
}
