package io.github.dirnei.mc.servus.manufacturing.menu;

import io.github.dirnei.mc.servus.manufacturing.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.machine.smelter.BurnerSmelterBlockEntity;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

public class BurnerSmelterMenu extends MachineMenu {

    public BurnerSmelterMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.BURNER_SMELTER.get(), containerId, playerInventory, machine);
    }

    private BurnerSmelterMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.BURNER_SMELTER.get(), containerId, playerInventory, machine, data);
    }

    public static BurnerSmelterMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new BurnerSmelterMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData());
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, BurnerSmelterBlockEntity.SLOT_INPUT, 56, 17));
        addSlot(new MachineSlot(machine, BurnerSmelterBlockEntity.SLOT_FUEL, 56, 53));
        addSlot(new OutputSlot(machine, BurnerSmelterBlockEntity.SLOT_OUTPUT, 116, 35));
    }
}
