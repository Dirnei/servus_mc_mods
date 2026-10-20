package io.github.dirnei.mc.servus.delicacies.firebox;

import java.util.function.Supplier;

/**
 * What heats a firebox: its wood fire, or the Heating Element once one is installed. Pure Java. With the element the
 * fire asks for no fuel; installing it puts a burning fire out, taking it out drops its heat.
 */
public final class FireboxHeat implements HeatSource {

    private final FuelFire fire;
    private final ElectricHeat electric;
    private boolean element;

    public FireboxHeat(FuelSupply fuel, EnergyDraw power, Supplier<HeatingStats> stats) {
        this.fire = new FuelFire(fuel);
        this.electric = new ElectricHeat(power, stats);
    }

    private HeatSource inUse() {
        return element ? electric : fire;
    }

    @Override
    public void tick(boolean demanded) {
        inUse().tick(demanded);
    }

    @Override
    public boolean hot() {
        return inUse().hot();
    }

    @Override
    public float heat() {
        return inUse().heat();
    }

    public boolean hasElement() {
        return element;
    }

    /** Installs the element; false when there already is one. */
    public boolean installElement() {
        if (element) {
            return false;
        }
        element = true;
        fire.restore(0, 0);
        electric.restore(0);
        return true;
    }

    /** Takes the element out; false when there is none. */
    public boolean removeElement() {
        if (!element) {
            return false;
        }
        element = false;
        electric.restore(0);
        return true;
    }

    /** The change of heat per tick the gauge follows; 0 for a wood fire, which has no gauge. */
    public float rate() {
        return element ? electric.rate() : 0;
    }

    public FuelFire fire() {
        return fire;
    }

    public ElectricHeat electric() {
        return electric;
    }

    /** Restores a saved firebox. */
    public void restore(boolean element, int burnLeft, int burnTotal, float heat) {
        this.element = element;
        fire.restore(burnLeft, burnTotal);
        electric.restore(element ? heat : 0);
    }
}
