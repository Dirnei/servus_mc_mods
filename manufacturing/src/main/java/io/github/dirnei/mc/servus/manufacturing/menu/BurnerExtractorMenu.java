package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.extractor.BurnerExtractorBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class BurnerExtractorMenu extends MachineMenu {

    public BurnerExtractorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.BURNER_EXTRACTOR.get(), containerId, playerInventory, machine);
    }

    private BurnerExtractorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.BURNER_EXTRACTOR.get(), containerId, playerInventory, machine, data);
    }

    public static BurnerExtractorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new BurnerExtractorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, BurnerExtractorBlockEntity.SLOT_FUEL, 56, 53));
        addSlot(new OutputSlot(machine, BurnerExtractorBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
