package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.generator.CoalGeneratorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class CoalGeneratorMenu extends MachineMenu {

    public CoalGeneratorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.COAL_GENERATOR.get(), containerId, playerInventory, machine);
    }

    private CoalGeneratorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.COAL_GENERATOR.get(), containerId, playerInventory, machine, data);
    }

    public static CoalGeneratorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new CoalGeneratorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, CoalGeneratorBlockEntity.SLOT_FUEL, 56, 53));
    }
}
