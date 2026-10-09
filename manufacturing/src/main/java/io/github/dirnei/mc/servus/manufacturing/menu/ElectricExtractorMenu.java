package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.ElectricExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class ElectricExtractorMenu extends MachineMenu {

    public ElectricExtractorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.ELECTRIC_EXTRACTOR.get(), containerId, playerInventory, machine);
    }

    private ElectricExtractorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.ELECTRIC_EXTRACTOR.get(), containerId, playerInventory, machine, data);
    }

    public static ElectricExtractorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new ElectricExtractorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new OutputSlot(machine, ElectricExtractorBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
