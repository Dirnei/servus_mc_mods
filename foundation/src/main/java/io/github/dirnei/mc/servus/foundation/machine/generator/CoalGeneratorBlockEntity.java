package io.github.dirnei.mc.servus.foundation.machine.generator;

import io.github.dirnei.mc.servus.foundation.transfer.Transfer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.dirnei.mc.servus.foundation.ServusFoundationIds;
import io.github.dirnei.mc.servus.foundation.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.EnergyNeighbors;
import io.github.dirnei.mc.servus.foundation.machine.FoundationMachineKinds;
import io.github.dirnei.mc.servus.foundation.naming.MachineNameSlot;
import io.github.dirnei.mc.servus.foundation.naming.NamedMachine;
import io.github.dirnei.mc.servus.foundation.machine.MachineEnergyStorage;
import io.github.dirnei.mc.servus.foundation.machine.MachineDefaults;
import io.github.dirnei.mc.servus.foundation.machine.MachineStats;
import io.github.dirnei.mc.servus.foundation.machine.menu.CoalGeneratorMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.RestartGate;
import io.github.dirnei.mc.servus.foundation.side.RelativeSide;
import io.github.dirnei.mc.servus.foundation.side.SideMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns furnace fuel into FE. Works like a burner machine whose work cycle is
 * one tick that adds {@code energyPerTick} to the buffer, then pushes energy
 * to its neighbours. Once the buffer is full it waits until the buffer drops
 * below its restart threshold.
 */
public class CoalGeneratorBlockEntity extends BurnerMachineBlockEntity implements NamedMachine {

    public static final int SLOT_FUEL = 0;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(ServusFoundationIds.MOD_ID, "coal_generator");
    private static final MachineStats DEFAULTS = MachineDefaults.stats(ID.toString());

    private final MachineNameSlot nameSlot = new MachineNameSlot(this, FoundationMachineKinds.COAL_GENERATOR);

    private final MachineEnergyStorage energy;
    private final RestartGate restart = new RestartGate();
    private EnergyNeighbors neighbors;
    private boolean capacityApplied;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.COAL_GENERATOR_ENTITY.get(), pos, state, 1);
        this.energy = new MachineEnergyStorage(DEFAULTS.energyCapacity(), 0, Integer.MAX_VALUE, this::setChanged);
        // The chimney sits on top: no fuel passes there.
        sideConfig().lock(RelativeSide.TOP, SideMode.DISABLED);
    }

    public MachineEnergyStorage energyStorage() {
        return energy;
    }

    @Override
    public MachineNameSlot nameSlot() {
        return nameSlot;
    }

    @Override
    protected Identifier machineId() {
        return ID;
    }

    @Override
    protected MachineStats defaultStats() {
        return DEFAULTS;
    }

    @Override
    protected int fuelSlot() {
        return SLOT_FUEL;
    }

    @Override
    protected boolean canWork() {
        boolean noRoomForATick = energy.getMaxEnergyStored() - energy.getEnergyStored() < stats().energyPerTick();
        return restart.allows(noRoomForATick, energy.getEnergyStored(), energy.getMaxEnergyStored(),
                stats().restartBelow());
    }

    @Override
    protected boolean restartWaiting() {
        return restart.waiting();
    }

    @Override
    protected void finishWork() {
        energy.add(stats().energyPerTick());
    }

    @Override
    protected boolean tick() {
        if (!capacityApplied) {
            energy.setCapacity(stats().energyCapacity());
            capacityApplied = true;
        }
        boolean working = super.tick();
        pushEnergy();
        return working;
    }

    private void pushEnergy() {
        if (energy.getEnergyStored() <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        if (neighbors == null) {
            // The top carries the chimney: never push upward.
            neighbors = new EnergyNeighbors(server, worldPosition, Direction.UP);
        }
        int rate = stats().energyPerTick();
        neighbors.forEach((direction, storage) -> {
            int offered = Math.min(rate, energy.getEnergyStored());
            if (offered <= 0) {
                return;
            }
            int accepted = Transfer.receiveEnergy(storage, offered, true);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
                Transfer.receiveEnergy(storage, accepted, false);
            }
        });
    }

    @Override
    protected int dataCount() {
        return DATA_COUNT_RESTART;
    }

    @Override
    protected int getData(int index) {
        return switch (index) {
            case DATA_ENERGY -> energy.getEnergyStored();
            case DATA_ENERGY_CAPACITY -> energy.getMaxEnergyStored();
            case DATA_RESTART_LEVEL -> (int) RestartGate.level(energy.getMaxEnergyStored(), stats().restartBelow());
            default -> super.getData(index);
        };
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", energy.getEnergyStored());
        output.putBoolean("RestartWaiting", restart.waiting());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.setEnergy(input.getIntOr("Energy", 0));
        restart.setWaiting(input.getBooleanOr("RestartWaiting", false));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this);
    }
}
