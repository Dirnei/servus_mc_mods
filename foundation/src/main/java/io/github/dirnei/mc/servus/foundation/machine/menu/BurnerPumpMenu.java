package io.github.dirnei.mc.servus.foundation.machine.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.pump.BurnerPumpBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** Fuel slot plus the synced water buffer and source flag. */
public class BurnerPumpMenu extends MachineMenu {

    public BurnerPumpMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.BURNER_PUMP_MENU.get(), containerId, playerInventory, machine);
    }

    private BurnerPumpMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.BURNER_PUMP_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static BurnerPumpMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new BurnerPumpMenu(containerId, playerInventory, readMachine(playerInventory, buf),
                clientData(MachineBlockEntity.DATA_COUNT_FLUID));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, BurnerPumpBlockEntity.SLOT_FUEL, 56, 53));
    }
}
