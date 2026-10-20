package io.github.dirnei.mc.servus.delicacies.firebox;

import io.github.dirnei.mc.servus.foundation.machine.MachineEnergyStorage;
import io.github.dirnei.mc.servus.foundation.registry.ServusFoundationItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * The heat of a firebox block entity: its wood fire, and the Heating Element with its FE buffer once one is installed.
 * The owner stores {@link #heat()} as its heat source, ticks it through {@link #tick(boolean)} on the server, and sends
 * clients the gauge when the tick asks for it; on the client {@link #shown(long, float)} is the needle's reading.
 */
public final class FireboxHeating {

    private final Identifier statsId;
    private final HeatingStats defaults;
    private final MachineEnergyStorage energy;
    private final FireboxHeat heat;
    private final GaugeSync sync = new GaugeSync();
    private float readHeat;
    private float readRate;
    private long readTime = -1;

    /**
     * @param statsId   the firebox's id in {@code data/<ns>/heating/}
     * @param defaults  its numbers when no datapack names it
     * @param fuel      the wood fire's fuel
     * @param onChanged runs when something to save changed
     */
    public FireboxHeating(Identifier statsId, HeatingStats defaults, FuelSupply fuel, Runnable onChanged) {
        this.statsId = statsId;
        this.defaults = defaults;
        this.energy = new MachineEnergyStorage(defaults.energyBuffer(), Integer.MAX_VALUE, 0, onChanged);
        this.heat = new FireboxHeat(fuel, energy::consume, this::stats);
    }

    /** The firebox's numbers as the datapack sets them now. */
    public HeatingStats stats() {
        return HeatingStatsManager.INSTANCE.get(statsId, defaults);
    }

    /** The heat source the firebox answers {@link Firebox#isHot()} from. */
    public HeatSource heat() {
        return heat;
    }

    /** The wood fire, for saving and status. */
    public FuelFire fire() {
        return heat.fire();
    }

    public boolean hasElement() {
        return heat.hasElement();
    }

    /** Whether new fuel may go in: not while the element heats. */
    public boolean acceptsFuel() {
        return !heat.hasElement();
    }

    /** One server tick; returns whether clients need a new gauge reading. */
    public boolean tick(boolean demanded) {
        if (heat.hasElement()) {
            energy.setCapacity(stats().energyBuffer());
        }
        heat.tick(demanded);
        return heat.hasElement() && sync.changed(heat.heat(), heat.hot(), heat.rate());
    }

    /** Installs the element; false when there already is one. */
    public boolean install() {
        if (!heat.installElement()) {
            return false;
        }
        energy.setCapacity(stats().energyBuffer());
        energy.setEnergy(0);
        sync.reset();
        return true;
    }

    /** Takes the element out with its heat and FE; returns the item, or empty when there was none. */
    public ItemStack takeElement() {
        if (!heat.removeElement()) {
            return ItemStack.EMPTY;
        }
        energy.setEnergy(0);
        sync.reset();
        return new ItemStack(ServusFoundationItems.HEATING_ELEMENT.get());
    }

    /** Drops the element at {@code pos}, when there is one. */
    public void dropElement(Level level, BlockPos pos) {
        ItemStack element = takeElement();
        if (!element.isEmpty()) {
            Block.popResource(level, pos, element);
        }
    }

    /** The FE buffer while the element is in, else null. */
    public @Nullable EnergyHandler energy() {
        return heat.hasElement() ? energy : null;
    }

    /** FE stored in the buffer. */
    public int storedEnergy() {
        return energy.getEnergyStored();
    }

    /** The heat in percent, for status messages. */
    public int heatPercent() {
        return Math.round(heat.heat() * 100);
    }

    /** Sets the element's temperature, for GameTests that start from a hot firebox. */
    public void setHeat(float value) {
        heat.electric().restore(value);
    }

    /** The threshold in percent, for status messages. */
    public int hotAtPercent() {
        return Math.round(stats().hotAt() * 100);
    }

    public void save(ValueOutput output) {
        output.putBoolean("Element", heat.hasElement());
        output.putInt("BurnLeft", heat.fire().burnLeft());
        output.putInt("BurnTotal", heat.fire().burnTotal());
        output.putFloat("Heat", heat.electric().heat());
        output.putInt("Energy", energy.getEnergyStored());
    }

    /** What clients need for the look and the gauge. */
    public void saveGauge(ValueOutput output) {
        output.putBoolean("Element", heat.hasElement());
        output.putFloat("Heat", heat.electric().heat());
        output.putFloat("HeatRate", heat.rate());
    }

    public void load(ValueInput input) {
        boolean element = input.getBooleanOr("Element", false);
        float saved = input.getFloatOr("Heat", 0);
        heat.restore(element, input.getIntOr("BurnLeft", 0), input.getIntOr("BurnTotal", 0), saved);
        energy.setCapacity(stats().energyBuffer());
        energy.setEnergy(input.getIntOr("Energy", 0));
        readHeat = element ? saved : 0;
        readRate = element ? input.getFloatOr("HeatRate", 0) : 0;
        readTime = -1;
    }

    /** The heat the gauge shows at {@code gameTime}, carried on from the last reading by its rate. */
    public float shown(long gameTime, float partialTick) {
        if (readTime < 0) {
            readTime = gameTime;
        }
        return GaugeNeedle.shown(readHeat, readRate, gameTime - readTime + partialTick);
    }
}
