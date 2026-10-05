package io.github.dirnei.mc.servus.manufacturing.machine.generator;

import io.github.dirnei.mc.servus.manufacturing.ServusManufacturingIds;
import io.github.dirnei.mc.servus.manufacturing.machine.BurnerMachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.EnergyNeighbors;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineEnergyStorage;
import io.github.dirnei.mc.servus.manufacturing.machine.MachineStats;
import io.github.dirnei.mc.servus.manufacturing.menu.CoalGeneratorMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Burns furnace fuel into FE. Works like a burner machine whose work cycle is
 * one tick that adds {@code energyPerTick} to the buffer, then pushes energy
 * to its neighbours.
 */
public class CoalGeneratorBlockEntity extends BurnerMachineBlockEntity {

    public static final int SLOT_FUEL = 0;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ServusManufacturingIds.MOD_ID, "coal_generator");
    private static final MachineStats DEFAULTS = MachineStats.generator(40, 16000, 4);

    private final MachineEnergyStorage energy;
    private EnergyNeighbors neighbors;
    private boolean capacityApplied;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ServusManufacturingBlockEntities.COAL_GENERATOR.get(), pos, state, 1);
        this.energy = new MachineEnergyStorage(DEFAULTS.energyCapacity(), 0, Integer.MAX_VALUE, this::setChanged);
    }

    public MachineEnergyStorage energyStorage() {
        return energy;
    }

    @Override
    protected ResourceLocation machineId() {
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
    protected boolean isInputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    protected boolean acceptsInput(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected boolean canWork() {
        return !energy.isFull();
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
            neighbors = new EnergyNeighbors(server, worldPosition);
        }
        int rate = stats().energyPerTick();
        neighbors.forEach((direction, storage) -> {
            if (!storage.canReceive()) {
                return;
            }
            int offered = Math.min(rate, energy.getEnergyStored());
            if (offered <= 0) {
                return;
            }
            int accepted = storage.receiveEnergy(offered, true);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
                storage.receiveEnergy(accepted, false);
            }
        });
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CoalGeneratorMenu(containerId, playerInventory, this);
    }
}
