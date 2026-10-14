package io.github.dirnei.mc.servus.manufacturing.alchemy;

import io.github.dirnei.mc.servus.foundation.machine.MachineBlockEntity;
import io.github.dirnei.mc.servus.foundation.machine.menu.MachineMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;

/** A machine menu that syncs three principle tanks in the {@link AlchemyMachines} data slots for its screen's gauges. */
public abstract class PrincipleMenu extends MachineMenu {

    protected PrincipleMenu(MenuType<?> type, int containerId, Inventory playerInventory, MachineBlockEntity machine) {
        super(type, containerId, playerInventory, machine);
    }

    protected PrincipleMenu(MenuType<?> type, int containerId, Inventory playerInventory, MachineBlockEntity machine, ContainerData data) {
        super(type, containerId, playerInventory, machine, data);
    }

    public int principleAmount(Principle principle) {
        return data().get(AlchemyMachines.DATA_PRINCIPLE_FIRST + principle.ordinal());
    }

    public int principleCapacity() {
        return data().get(AlchemyMachines.DATA_TANK_CAPACITY);
    }
}
