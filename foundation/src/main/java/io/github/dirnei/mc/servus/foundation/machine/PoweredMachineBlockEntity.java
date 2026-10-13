package io.github.dirnei.mc.servus.foundation.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A machine that runs on FE. Accepts energy from every side, spends
 * {@code energyPerTick} while it has work, and keeps its progress when the
 * buffer runs dry.
 */
public abstract class PoweredMachineBlockEntity extends MachineBlockEntity {

    protected final MachineEnergyStorage energy;
    private boolean capacityApplied;

    protected PoweredMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        this.energy = new MachineEnergyStorage(defaultStats().energyCapacity(), Integer.MAX_VALUE, 0, this::setChanged);
    }

    /** Whether the machine has something to do right now (input present, output has room). */
    protected abstract boolean canWork();

    /** Performs one finished work cycle. Only called when {@link #canWork()} was true. */
    protected abstract void finishWork();

    /** FE spent per working tick right now; machines whose cost depends on their state override. */
    protected int energyPerTickNow() {
        return stats().energyPerTick();
    }

    public MachineEnergyStorage energyStorage() {
        return energy;
    }

    @Override
    protected boolean tick() {
        if (!capacityApplied) {
            energy.setCapacity(stats().energyCapacity());
            capacityApplied = true;
        }
        if (!canWork()) {
            resetProgress();
            return false;
        }
        if (!energy.consume(energyPerTickNow())) {
            return false;
        }
        if (stepProgress()) {
            finishWork();
        }
        return true;
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_ENERGY -> energy.getEnergyStored();
            case DATA_ENERGY_CAPACITY -> energy.getMaxEnergyStored();
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
    }
}
