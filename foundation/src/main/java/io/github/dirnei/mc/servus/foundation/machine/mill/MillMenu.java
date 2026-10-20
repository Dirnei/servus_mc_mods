package io.github.dirnei.mc.servus.foundation.machine.mill;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.MachineContent;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Mill's input and output, with the work collected as the progress arrow. */
public class MillMenu extends MachineMenu {

    public MillMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(MachineContent.MILL_MENU.get(), containerId, playerInventory, machine);
    }

    private MillMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(MachineContent.MILL_MENU.get(), containerId, playerInventory, machine, data);
    }

    public static MillMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new MillMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, MillBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new OutputSlot(machine, MillBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
