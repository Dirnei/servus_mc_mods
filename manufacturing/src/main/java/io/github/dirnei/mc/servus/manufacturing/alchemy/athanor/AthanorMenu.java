package io.github.dirnei.mc.servus.manufacturing.alchemy.athanor;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineSlot;
import io.github.dirnei.mc.servus.foundation.machine.menu.OutputSlot;
import io.github.dirnei.mc.servus.manufacturing.alchemy.AlchemyMachines;
import io.github.dirnei.mc.servus.manufacturing.alchemy.Principle;
import io.github.dirnei.mc.servus.manufacturing.alchemy.PrincipleMenu;
import io.github.dirnei.mc.servus.manufacturing.registry.ServusManufacturingMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** The Athanor's item and result slots, its tanks and its tier. */
public class AthanorMenu extends MachineMenu implements PrincipleMenu {

    public AthanorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(ServusManufacturingMenus.ATHANOR.get(), containerId, playerInventory, machine);
    }

    private AthanorMenu(int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(ServusManufacturingMenus.ATHANOR.get(), containerId, playerInventory, machine, data);
    }

    public static AthanorMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new AthanorMenu(containerId, playerInventory, readMachine(playerInventory, buf), clientData(AthanorBlockEntity.DATA_COUNT));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new MachineSlot(machine, AthanorBlockEntity.SLOT_INPUT, 56, 35));
        addSlot(new OutputSlot(machine, AthanorBlockEntity.SLOT_OUTPUT, 116, 35));
    }

    @Override
    public int principleAmount(Principle principle) {
        return data().get(AlchemyMachines.DATA_PRINCIPLE_FIRST + principle.ordinal());
    }

    @Override
    public int principleCapacity() {
        return data().get(AlchemyMachines.DATA_TANK_CAPACITY);
    }

    public int tier() {
        return data().get(AthanorBlockEntity.DATA_TIER);
    }
}
