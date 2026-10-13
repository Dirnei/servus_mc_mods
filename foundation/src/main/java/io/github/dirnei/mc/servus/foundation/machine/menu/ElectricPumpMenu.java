package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** No slots: the synced energy, water buffer and source flag. */
public class ElectricPumpMenu extends MachineMenu {

    public ElectricPumpMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.ELECTRIC_PUMP_MENU.get(), containerId, playerInventory, machine);
    }

    private ElectricPumpMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.ELECTRIC_PUMP_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static ElectricPumpMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricPumpMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                clientData(MachineBlockEntity.DATA_COUNT_FLUID));
    }

    @Override
    protected void addMachineSlots() {
    }
}
