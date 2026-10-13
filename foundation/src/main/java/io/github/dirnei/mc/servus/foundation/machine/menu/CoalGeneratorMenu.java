package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** Fuel slot plus the synced energy buffer. */
public class CoalGeneratorMenu extends MachineMenu {

    public CoalGeneratorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.COAL_GENERATOR_MENU.get(), containerId, playerInventory, machine);
    }

    private CoalGeneratorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.COAL_GENERATOR_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static CoalGeneratorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CoalGeneratorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CoalGeneratorBlockEntity.SLOT_FUEL, 56, 53));
    }
}
