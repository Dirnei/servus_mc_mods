package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.crusher.CrusherBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class CrusherMenu extends MachineMenu {

    public CrusherMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.CRUSHER.get(), containerId, playerInventory, machine);
    }

    private CrusherMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.CRUSHER.get(), containerId, playerInventory, machine, data);
    }

    public static CrusherMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CrusherMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CrusherBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new OutputSlot(machine, CrusherBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
