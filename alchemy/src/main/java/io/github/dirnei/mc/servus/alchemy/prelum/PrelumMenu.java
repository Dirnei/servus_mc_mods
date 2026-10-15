package io.github.dirnei.mc.servus.alchemy.prelum;

import io.github.dirnei.mc.servus.alchemy.registry.ServusAlchemyMenus;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Prelum's input and output slots on either side of the arrow. */
public class PrelumMenu extends MachineMenu {

    public PrelumMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusAlchemyMenus.PRELUM.get(), containerId, playerInventory, machine);
    }

    private PrelumMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusAlchemyMenus.PRELUM.get(), containerId, playerInventory, machine, data);
    }

    public static PrelumMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new PrelumMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, PrelumBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new OutputSlot(machine, PrelumBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
